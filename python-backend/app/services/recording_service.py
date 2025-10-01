# app/services/recording_service.py
import subprocess
from pathlib import Path
from typing import Dict, Any, Tuple
import time

from .stt_service import transcribe_audio
from .db_service import (
    save_transcript,
    save_summary,
    update_calendar_contents,
)
from .ai_service import generate_summary

async def process_complete_recording(session_data: Dict[str, Any]) -> Dict[str, Any]:
    """
    완전 처리 파이프라인:
    1. 오디오 청크 병합
    2. Whisper STT 전사
    3. 전사 결과 DB 저장 + 달력 업데이트
    4. Ollama AI 요약 생성
    5. 요약 DB 저장 + 달력 최종 업데이트
    """
    room_code = session_data["room_code"]
    room_no = session_data["room_no"]
    session_dir = session_data["session_dir"]

    results = {
        "room_code": room_code,
        "room_no": room_no,
        "success": False,
        "steps": {},
        "errors": [],
        "total_time": 0,
    }

    print(f"[RECORDING] 완전 처리 시작: {room_code}")

    # room_no가 None이면 DB에서 조회
    if room_no is None:
        print(f"[RECORDING] room_no 조회 중: {room_code}")
        try:
            from .db_service import engine
            from sqlalchemy import text

            with engine.connect() as conn:
                query = text(
                    "SELECT room_no FROM TB_MEETING_ROOM WHERE room_code = :room_code"
                )
                result = conn.execute(query, {"room_code": room_code})
                row = result.fetchone()
                if row:
                    room_no = row[0]
                    session_data["room_no"] = room_no
                    results["room_no"] = room_no
                    print(f"[RECORDING] room_no 조회 성공: {room_no}")
                else:
                    error_msg = (
                        f"roomCode {room_code}에 해당하는 회의를 찾을 수 없습니다"
                    )
                    results["errors"].append(error_msg)
                    return results
        except Exception as e:
            error_msg = f"room_no 조회 실패: {e}"
            results["errors"].append(error_msg)
            return results

    start_time = time.time()

    try:
        # STEP 1: 오디오 파일 병합
        print(f"[RECORDING] STEP 1: 오디오 파일 병합")
        merged_file = await merge_audio_chunks(session_dir)
        results["steps"]["merge"] = {
            "success": True,
            "file_path": str(merged_file),
            "file_size": merged_file.stat().st_size,
        }

        # STEP 2: STT 전사
        print(f"[RECORDING] STEP 2: STT 전사")
        transcript_text, stt_time = transcribe_audio(merged_file, language="ko")

        if not transcript_text or not transcript_text.strip():
            raise ValueError("전사 결과가 비어있습니다")

        results["steps"]["stt"] = {
            "success": True,
            "text_length": len(transcript_text),
            "processing_time": stt_time,
            "preview": transcript_text[:100],
        }

        # STEP 3: 전사 결과 저장 + 달력 1차 업데이트
        print(f"[RECORDING] STEP 3: 전사 결과 저장")
        transcript_saved = save_transcript(room_no, transcript_text)
        calendar_updated_1 = update_calendar_contents(room_no, transcript_text)

        results["steps"]["save_transcript"] = {
            "success": transcript_saved and calendar_updated_1,
            "transcript_saved": transcript_saved,
            "calendar_updated": calendar_updated_1,
        }

        # STEP 4: AI 요약 (Ollama)
        print(f"[RECORDING] STEP 4: AI 요약 생성")
        try:
            summary_full, action_items, decisions = generate_summary(transcript_text)

            results["steps"]["ai_summary"] = {
                "success": True,
                "summary_length": len(summary_full),
                "action_items_length": len(action_items),
                "decisions_length": len(decisions),
            }

        except Exception as e:
            print(f"[RECORDING] AI 요약 실패: {e}")
            # 폴백: 간단한 요약
            summary_full = f"회의 내용: {transcript_text[:300]}..."
            action_items = "- [ ] AI 요약 실패로 인한 수동 정리 필요"
            decisions = "- AI 요약 실패로 인한 수동 확인 필요"

            results["steps"]["ai_summary"] = {
                "success": False,
                "error": str(e),
                "fallback_used": True,
            }

        # STEP 5: 요약 저장 + 달력 2차 업데이트
        print(f"[RECORDING] STEP 5: 요약 저장")
        summary_saved = save_summary(room_no, summary_full, action_items, decisions)

        # 최종 회의록 생성
        full_meeting_record = f"""{summary_full}

## 액션 아이템
{action_items}

## 결정 사항
{decisions}"""

        calendar_updated_2 = update_calendar_contents(room_no, full_meeting_record)

        results["steps"]["save_summary"] = {
            "success": summary_saved and calendar_updated_2,
            "summary_saved": summary_saved,
            "calendar_updated": calendar_updated_2,
        }

        # 전체 성공 여부 판단
        critical_steps = ["merge", "stt", "save_transcript"]
        results["success"] = all(
            results["steps"].get(step, {}).get("success", False)
            for step in critical_steps
        )

        results["ai_summary_failed"] = not results["steps"].get("ai_summary", {}).get("success", False)
        if results["ai_summary_failed"]:
            print(f"[RECORDING] WARNING: AI 요약 실패했지만 오디오 파일은 보존됨")

        results["total_time"] = time.time() - start_time

        print(
            f"[RECORDING] 완전 처리 완료: {results['success']}, {results['total_time']:.2f}초"
        )
        return results

    except Exception as e:
        error_msg = f"{type(e).__name__}: {str(e)}"
        results["errors"].append(error_msg)
        results["total_time"] = time.time() - start_time

        print(f"[RECORDING] 완전 처리 실패: {error_msg}")
        import traceback

        traceback.print_exc()
        return results


async def merge_audio_chunks(session_dir: Path) -> Path:
    """WebM 청크들을 WAV 파일로 병합"""
    session_path = Path(session_dir)
    chunks_dir = session_path / "chunks"
    merged_dir = session_path / "merged"
    merged_dir.mkdir(exist_ok=True)
    merged_file = merged_dir / "merged_audio.wav"

    print(f"[MERGE] 오디오 병합 시작: {chunks_dir}")

    # 청크 파일 검색 및 검증
    chunk_files = sorted(chunks_dir.glob("chunk_*.webm"))
    print(f"[MERGE] 찾은 청크 파일: {len(chunk_files)}개")

    if not chunk_files:
        raise FileNotFoundError("병합할 청크 파일이 없습니다")

    # 유효한 청크 파일만 선별 (1KB 이상)
    valid_chunks = []
    for chunk in chunk_files:
        if chunk.exists() and chunk.stat().st_size > 1024:
            valid_chunks.append(chunk)

    if not valid_chunks:
        raise ValueError("유효한 청크 파일이 없습니다")

    print(f"[MERGE] 유효한 청크: {len(valid_chunks)}개")

    try:
        if len(valid_chunks) == 1:
            # 단일 파일 변환
            cmd = [
                "ffmpeg",
                "-y",
                "-i",
                str(valid_chunks[0]),
                "-acodec",
                "pcm_s16le",
                "-ar",
                "22050",
                "-ac",
                "1",
                str(merged_file),
            ]
        else:
            # 다중 파일 병합
            file_list = session_path / "file_list.txt"
            with open(file_list, "w", encoding="utf-8") as f:
                for chunk in valid_chunks:
                    chunk_path = str(chunk.absolute()).replace("\\", "/")
                    f.write(f"file '{chunk_path}'\n")

            # 1단계: concat으로 병합
            temp_merged = merged_dir / "temp_merged.webm"
            cmd_concat = [
                "ffmpeg",
                "-y",
                "-f",
                "concat",
                "-safe",
                "0",
                "-i",
                str(file_list),
                "-c",
                "copy",
                str(temp_merged),
            ]

            result = subprocess.run(cmd_concat, capture_output=True, text=True)
            if result.returncode != 0:
                raise subprocess.CalledProcessError(
                    result.returncode, cmd_concat, result.stderr
                )

            # 2단계: WAV로 변환
            cmd = [
                "ffmpeg",
                "-y",
                "-i",
                str(temp_merged),
                "-acodec",
                "pcm_s16le",
                "-ar",
                "22050",
                "-ac",
                "1",
                str(merged_file),
            ]

            # 임시 파일 정리
            try:
                file_list.unlink()
            except:
                pass

        # FFmpeg 실행
        print(f"[MERGE] FFmpeg 실행")
        result = subprocess.run(cmd, capture_output=True, text=True)

        if result.returncode != 0:
            raise subprocess.CalledProcessError(result.returncode, cmd, result.stderr)

        if not merged_file.exists():
            raise FileNotFoundError("병합된 파일이 생성되지 않았습니다")

        final_size = merged_file.stat().st_size
        print(f"[MERGE] 병합 완료: {final_size:,} bytes")

        # 임시 파일 정리
        if len(valid_chunks) > 1:
            temp_merged = merged_dir / "temp_merged.webm"
            if temp_merged.exists():
                try:
                    temp_merged.unlink()
                except:
                    pass

        return merged_file

    except subprocess.CalledProcessError as e:
        print(f"[MERGE] FFmpeg 실행 실패: {e}")
        raise e
    except Exception as e:
        print(f"[MERGE] 병합 실패: {e}")
        raise e


def cleanup_session_files(session_dir: Path, keep_merged: bool = True):
    """세션 파일들 정리"""
    try:
        session_path = Path(session_dir)

        # 청크 파일들 삭제
        chunks_dir = session_path / "chunks"
        if chunks_dir.exists():
            for chunk_file in chunks_dir.glob("chunk_*.webm"):
                try:
                    chunk_file.unlink()
                except:
                    pass
            try:
                chunks_dir.rmdir()
            except:
                pass

        # 병합 파일 처리
        if not keep_merged:
            merged_dir = session_path / "merged"
            if merged_dir.exists():
                for merged_file in merged_dir.glob("*"):
                    try:
                        merged_file.unlink()
                    except:
                        pass
                try:
                    merged_dir.rmdir()
                except:
                    pass

        # 메타데이터 파일 삭제
        metadata_file = session_path / "metadata.json"
        if metadata_file.exists():
            try:
                metadata_file.unlink()
            except:
                pass

        # 빈 세션 디렉토리 삭제
        try:
            if not any(session_path.iterdir()):
                session_path.rmdir()
        except:
            pass

        print(f"[CLEANUP] 세션 정리 완료: {session_dir}")

    except Exception as e:
        print(f"[CLEANUP] 세션 정리 실패: {e}")
