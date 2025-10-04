# app/routers/recording.py
from fastapi import APIRouter, File, UploadFile, Form, HTTPException, BackgroundTasks, Request
from pydantic import BaseModel
from pathlib import Path
from typing import Optional, Dict, Any
import time
import json
from datetime import datetime

from app.config import AUDIO_DIR
from app.services.recording_service import (
    process_complete_recording,
    cleanup_session_files,
)

router = APIRouter()

# 전역 세션 관리 (메모리 기반)
recording_sessions = {}


class RecordingRequest(BaseModel):
    roomCode: str
    roomNo: Optional[int] = None
    token: Optional[str] = None

class RecordingResponse(BaseModel):
    success: bool
    message: str
    data: Optional[Dict] = None


class RecordingSession:
    """녹음 세션 관리 클래스"""

    def __init__(self, room_code: str, room_no: int):
        self.room_code = room_code
        self.room_no = room_no
        self.token = None
        self.status = "recording"  # recording, paused, processing, completed, error
        self.chunk_count = 0
        self.start_time = time.time()
        self.session_dir = AUDIO_DIR / room_code
        self.chunks_dir = self.session_dir / "chunks"
        self.chunks_dir.mkdir(parents=True, exist_ok=True)

    def get_next_chunk_path(self):
        """다음 청크 파일 경로 생성"""
        self.chunk_count += 1
        return self.chunks_dir / f"chunk_{self.chunk_count:03d}.webm"

    def save_metadata(self):
        """세션 메타데이터를 JSON 파일로 저장"""
        metadata = {
            "room_code": self.room_code,
            "room_no": self.room_no,
            "status": self.status,
            "chunk_count": self.chunk_count,
            "start_time": self.start_time,
            "last_updated": time.time(),
        }
        with open(self.session_dir / "metadata.json", "w") as f:
            json.dump(metadata, f, indent=2)

    def to_dict(self):
        """딕셔너리 형태로 변환 (백그라운드 작업 전달용)"""
        return {
            "room_code": self.room_code,
            "room_no": self.room_no,
            "token": self.token,
            "session_dir": str(self.session_dir),
            "status": self.status,
            "chunk_count": self.chunk_count,
            "start_time": self.start_time,
        }


@router.post("/stt/start-recording", response_model=RecordingResponse)
async def start_recording(request: RecordingRequest):
    """녹음 세션 시작 - 기존 데이터 확인 후 진행"""
    try:
        room_code = request.roomCode
        room_no = request.roomNo

        print(f"[RECORDING] 녹음 시작 요청: {room_code}, roomNo: {room_no}")

        # 1. 현재 진행 중인 녹음 세션 확인
        if room_code in recording_sessions:
            current_session = recording_sessions[room_code]
            if current_session.status in ['recording', 'paused']:
                return RecordingResponse(
                    success=False, 
                    message="이미 녹음이 진행 중입니다.",
                    data={
                        "current_status": current_session.status,
                        "chunk_count": current_session.chunk_count
                    }
                )
            elif current_session.status == 'processing':
                return RecordingResponse(
                    success=False,
                    message="이전 녹음이 아직 처리 중입니다. 잠시 후 다시 시도해주세요."
                )

        # 2. 기존 회의록 데이터 확인
        existing_data = check_existing_meeting_data(room_code)
        
        if existing_data.get("has_data"):
            if existing_data.get("status") == "processing":
                return RecordingResponse(
                    success=False,
                    message=existing_data.get("message", "이전 녹음이 처리 중입니다.")
                )
            elif existing_data.get("has_transcript"):
                return RecordingResponse(
                    success=False,
                    message="이미 회의록이 생성된 방입니다. 새로운 녹음을 시작할 수 없습니다.",
                    data={
                        "has_existing_data": True,
                        "room_no": existing_data.get("room_no")
                    }
                )

        # 3. 모든 검증을 통과한 경우 새 세션 생성
        session = RecordingSession(room_code, room_no)
        recording_sessions[room_code] = session
        session.save_metadata()

        print(f"[RECORDING] 새 세션 생성: {session.session_dir}")

        return RecordingResponse(
            success=True,
            message="녹음이 시작되었습니다.",
            data={
                "roomCode": room_code,
                "status": "recording",
                "sessionDir": str(session.session_dir),
                "room_no": room_no
            },
        )

    except Exception as e:
        print(f"[RECORDING] 시작 실패: {e}")
        import traceback
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/stt/upload-chunk")
async def upload_chunk(audio: UploadFile = File(...), roomCode: str = Form(...)):
    """실시간 오디오 청크 업로드"""
    try:
        print(f"[RECORDING] 청크 업로드: {roomCode}, 파일: {audio.filename}")

        if roomCode not in recording_sessions:
            raise HTTPException(status_code=404, detail="녹음 세션을 찾을 수 없습니다.")

        session = recording_sessions[roomCode]

        if session.status not in ["recording", "paused"]:
            raise HTTPException(
                status_code=400, detail=f"청크를 받을 수 없는 상태: {session.status}"
            )

        # 청크 파일 저장
        chunk_path = session.get_next_chunk_path()

        with open(chunk_path, "wb") as f:
            content = await audio.read()
            f.write(content)

        session.save_metadata()

        print(
            f"[RECORDING] 청크 저장 완료: {len(content)} bytes, 총 {session.chunk_count}개"
        )

        return {
            "success": True,
            "message": "청크가 저장되었습니다.",
            "chunkNumber": session.chunk_count,
            "chunkSize": len(content),
            "sessionStatus": session.status,
        }

    except HTTPException:
        raise
    except Exception as e:
        print(f"[RECORDING] 청크 업로드 실패: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/stt/pause-recording", response_model=RecordingResponse)
async def pause_recording(request: RecordingRequest):
    """녹음 일시정지"""
    try:
        room_code = request.roomCode

        if room_code not in recording_sessions:
            raise HTTPException(status_code=404, detail="녹음 세션을 찾을 수 없습니다.")

        session = recording_sessions[room_code]
        session.status = "paused"
        session.save_metadata()

        return RecordingResponse(
            success=True,
            message="녹음이 일시정지되었습니다.",
            data={"roomCode": room_code, "status": "paused"},
        )

    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/stt/resume-recording", response_model=RecordingResponse)
async def resume_recording(request: RecordingRequest):
    """녹음 재개"""
    try:
        room_code = request.roomCode

        if room_code not in recording_sessions:
            raise HTTPException(status_code=404, detail="녹음 세션을 찾을 수 없습니다.")

        session = recording_sessions[room_code]
        session.status = "recording"
        session.save_metadata()

        return RecordingResponse(
            success=True,
            message="녹음이 재개되었습니다.",
            data={"roomCode": room_code, "status": "recording"},
        )

    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/stt/stop-recording", response_model=RecordingResponse)
async def stop_recording(req: RecordingRequest, http: Request, background_tasks: BackgroundTasks):
    """녹음 종료 및 백그라운드 처리 시작"""
    try:
        auth = http.headers.get("Authorization", "")
        token = auth.split(" ", 1)[1].strip() if auth.lower().startswith("bearer ") else None

        if not token and req.token:
            token = req.token

        room_code = req.roomCode
        room_no = req.roomNo

        print(f"받은 토큰: {token[:50]}..." if token else "토큰 없음")
        print(f"[RECORDING] 녹음 종료 요청: {room_code}, roomNo: {room_no}")

        if room_code not in recording_sessions:
            raise HTTPException(status_code=404, detail="녹음 세션을 찾을 수 없습니다.")
        
        session = recording_sessions[room_code]
        session.status = "processing"
        session.token = token 
        session.save_metadata()
        
        print(f"[RECORDING] 총 {session.chunk_count}개 청크로 백그라운드 처리 시작")

        
        background_tasks.add_task(process_recording_background, session.to_dict())
        
        return RecordingResponse(
            success=True,
            message="녹음이 종료되었습니다. 회의록 생성 중입니다.",
            data={
                "roomCode": room_code,
                "status": "processing",
                "totalChunks": session.chunk_count,
            },
        )
        
    except HTTPException:
        raise
    except Exception as e:
        print(f"[RECORDING] 종료 실패: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/stt/sessions")
def get_active_sessions():
    """현재 활성 세션 조회 (디버깅용)"""
    sessions_info = {}
    for room_code, session in recording_sessions.items():
        sessions_info[room_code] = {
            "status": session.status,
            "chunk_count": session.chunk_count,
            "duration": time.time() - session.start_time,
        }

    return {"active_sessions": sessions_info, "total_count": len(recording_sessions)}


async def process_recording_background(session_data: Dict[str, Any]):
    """백그라운드 처리 함수 - 전체 파이프라인 실행"""
    room_code = session_data["room_code"]

    try:
        print(f"[BACKGROUND] 백그라운드 처리 시작: {room_code}")

        # 완전 처리 실행 (병합 → STT → 요약 → DB 저장)
        results = await process_complete_recording(session_data)

        # 처리 성공 시 알림 전송
        if results["success"]:
            await send_meeting_complete_notification(
                room_no=session_data["room_no"],
                room_code=room_code,
                token=session_data["token"]
            )

        # 세션 상태 업데이트
        if room_code in recording_sessions:
            session = recording_sessions[room_code]
            session.status = "completed" if results["success"] else "error"
            session.save_metadata()

        print(
            f"[BACKGROUND] 처리 완료: {room_code} - {'성공' if results['success'] else '실패'}"
        )

        # 파일 정리
        cleanup_session_files(Path(session_data["session_dir"]), keep_merged=True)

        # 메모리 세션 정리
        if room_code in recording_sessions:
            del recording_sessions[room_code]
            print(f"[CLEANUP] 메모리 세션 정리: {room_code}")

    except Exception as e:
        print(f"[BACKGROUND] 백그라운드 처리 실패: {room_code} - {e}")

        if room_code in recording_sessions:
            session = recording_sessions[room_code]
            session.status = "error"
            session.save_metadata()
            del recording_sessions[room_code]

        import traceback

        traceback.print_exc()

def check_existing_meeting_data(room_code: str) -> Dict:
    """기존 회의록 데이터 확인"""
    try:
        # 1. 진행 중인 세션 확인 (메모리)
        if room_code in recording_sessions:
            session = recording_sessions[room_code]
            if session.status in ['processing', 'completed']:
                return {
                    "has_data": True,
                    "status": "processing",
                    "message": "이전 녹음이 아직 처리 중입니다"
                }
        
        # 2. DB에 저장된 데이터 확인
        from app.services.db_service import engine
        from sqlalchemy import text
        
        with engine.connect() as conn:
            # room_no 조회
            room_query = text("SELECT room_no FROM TB_MEETING_ROOM WHERE room_code = :room_code")
            room_result = conn.execute(room_query, {"room_code": room_code})
            room_row = room_result.fetchone()
            
            if not room_row:
                return {"has_data": False}
            
            room_no = room_row[0]
            
            # 전사 데이터 확인
            transcript_query = text("SELECT transcript FROM TB_MEETING_TRANSCRIPT WHERE room_no = :room_no")
            transcript_result = conn.execute(transcript_query, {"room_no": room_no})
            transcript_row = transcript_result.fetchone()
            
            has_transcript = transcript_row and transcript_row[0]
            
            return {
                "has_data": has_transcript,
                "has_transcript": has_transcript,
                "room_no": room_no
            }
            
    except Exception as e:
        print(f"[RECORDING] 기존 데이터 확인 실패: {e}")
        return {"has_data": False}

async def send_meeting_complete_notification(room_no: int, room_code: str, token: str):
    """Spring 백엔드로 회의록 완료 알림 전송"""
    try:
        import httpx
        from app.config import SPRING_API_BASE_URL
        from app.services.db_service import engine
        from sqlalchemy import text

        print(f"[NOTIFICATION] 알림 전송 시작: room_no={room_no}, room_code={room_code}")

        # 1. 회의 제목 조회
        with engine.connect() as conn:
            query = text("SELECT title FROM TB_MEETING_ROOM WHERE room_no = :room_no")
            result = conn.execute(query, {"room_no": room_no})
            row = result.fetchone()
            meeting_title = row[0] if row else "회의"

        print(f"[NOTIFICATION] 회의 제목: {meeting_title}")

        # 2. 참가자 목록 조회
        with engine.connect() as conn:
            query = text("""
                SELECT user_no
                FROM TB_MEETING_PARTICIPANT
                WHERE room_no = :room_no
            """)
            result = conn.execute(query, {"room_no": room_no})
            participant_user_nos = [row[0] for row in result.fetchall()]
        
        print(f"[NOTIFICATION] 참가자 수: {len(participant_user_nos)}")

        # 3. Spring API 호출
        async with httpx.AsyncClient() as client:
            url = f"{SPRING_API_BASE_URL}/alarms/meeting-complete"
            payload = {
                "roomNo": room_no,
                "participantUserNos": participant_user_nos,
                "meetingTitle": meeting_title
            }

            headers = {
                "Authorization": f"Bearer {token}"
            }

            print(f"[NOTIFICATION] Spring API 호출: {url}")
            print(f"[NOTIFICATION] Payload: {payload}")
            print(f"[NOTIFICATION] Authorization 헤더 포함됨")
            response = await client.post(url, json=payload, headers=headers, timeout=10.0)            
            if response.status_code == 200:
                print(f"[NOTIFICATION] 알림 전송 성공: {room_code}")
            else:
                print(f"[NOTIFICATION] 알림 전송 실패: status={response.status_code}")
    
    except Exception as e:
        print(f"[NOTIFICATION] 알림 전송 중 오류: {e}")
        import traceback
        traceback.print_exc()