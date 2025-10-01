# app/services/stt_service.py
import os
import time
from pathlib import Path
from typing import Tuple
from faster_whisper import WhisperModel

from app.config import WHISPER_MODEL, WHISPER_DEVICE, CPU_THREADS

# CPU 성능 최적화 설정
os.environ.setdefault("OMP_NUM_THREADS", str(CPU_THREADS))
os.environ.setdefault("MKL_NUM_THREADS", str(CPU_THREADS))

# 전역 모델 인스턴스 (지연 로딩)
_whisper_model = None


def load_whisper_model():
    """Faster-Whisper 모델 지연 로딩"""
    global _whisper_model

    if _whisper_model is None:
        try:
            print(f"[STT] Faster-Whisper 모델 로딩 시작: {WHISPER_MODEL}")
            print(f"[STT] 디바이스: {WHISPER_DEVICE}")
            
            # 메모리 확인 (선택사항)
            try:
                import psutil
                mem = psutil.virtual_memory()
                print(f"[STT] 로딩 전 메모리 - 사용가능: {mem.available / (1024**3):.2f}GB, 사용률: {mem.percent}%")
            except ImportError:
                pass
            
            start_time = time.time()

            # faster-whisper 모델 로드
            _whisper_model = WhisperModel(
                WHISPER_MODEL,
                device=WHISPER_DEVICE,
                compute_type="int8" if WHISPER_DEVICE == "cpu" else "float16",
                cpu_threads=CPU_THREADS,
                num_workers=1
            )

            load_time = time.time() - start_time
            print(f"[STT] Faster-Whisper 모델 로딩 완료: {load_time:.2f}초")
            
            # 로딩 후 메모리 (선택사항)
            try:
                import psutil
                mem = psutil.virtual_memory()
                print(f"[STT] 로딩 후 메모리 - 사용가능: {mem.available / (1024**3):.2f}GB, 사용률: {mem.percent}%")
            except ImportError:
                pass

        except Exception as e:
            print(f"[STT] ❌ Whisper 모델 로딩 실패: {type(e).__name__}: {e}")
            import traceback
            traceback.print_exc()
            raise e

    return _whisper_model


def transcribe_audio(audio_path: Path, language: str = "ko") -> Tuple[str, float]:
    """
    오디오 파일을 텍스트로 전사

    Args:
        audio_path: 오디오 파일 경로
        language: 언어 코드 (기본값: "ko" 한국어)

    Returns:
        (전사된 텍스트, 처리 시간)
    """
    print(f"[STT] 음성 전사 시작: {audio_path}")
    print(f"[STT] 모델: {WHISPER_MODEL}, 디바이스: {WHISPER_DEVICE}")

    if not audio_path.exists():
        raise FileNotFoundError(f"오디오 파일을 찾을 수 없습니다: {audio_path}")

    file_size = audio_path.stat().st_size
    print(f"[STT] 파일 크기: {file_size:,} bytes")

    try:
        model = load_whisper_model()
        start_time = time.time()

        # faster-whisper 전사 실행
        segments, info = model.transcribe(
            str(audio_path),
            language=language,
            beam_size=5,
            vad_filter=True,  # 음성 구간만 전사
            vad_parameters=dict(min_silence_duration_ms=500)
        )

        # 세그먼트에서 텍스트 추출
        text_parts = []
        for segment in segments:
            text_parts.append(segment.text)

        text = " ".join(text_parts).strip()
        processing_time = time.time() - start_time

        print(f"[STT] 전사 완료: {processing_time:.2f}초, {len(text)}자")
        print(f"[STT] 감지 언어: {info.language}")
        print(f"[STT] 미리보기: {text[:100]}...")

        if not text:
            print(f"[STT] WARNING: 전사 결과가 비어있습니다")
            return "", processing_time

        return text, processing_time

    except Exception as e:
        print(f"[STT] 전사 실패: {type(e).__name__}: {e}")
        import traceback
        traceback.print_exc()
        raise e


def transcribe_audio_segments(audio_path: Path, language: str = "ko") -> dict:
    """
    타임스탬프가 포함된 세그먼트별 전사

    Args:
        audio_path: 오디오 파일 경로
        language: 언어 코드

    Returns:
        세그먼트 정보가 포함된 딕셔너리
    """
    print(f"[STT] 세그먼트별 전사 시작: {audio_path}")

    try:
        model = load_whisper_model()

        segments, info = model.transcribe(
            str(audio_path),
            language=language,
            beam_size=5,
            word_timestamps=True
        )

        # 세그먼트 정보 추출
        segments_list = []
        full_text = []
        
        for segment in segments:
            segments_list.append({
                "start": segment.start,
                "end": segment.end,
                "text": segment.text.strip()
            })
            full_text.append(segment.text)

        print(f"[STT] 세그먼트별 전사 완료: {len(segments_list)}개 세그먼트")

        return {
            "text": " ".join(full_text).strip(),
            "language": info.language,
            "segments": segments_list
        }

    except Exception as e:
        print(f"[STT] 세그먼트별 전사 실패: {e}")
        raise e


def get_model_info() -> dict:
    """현재 로드된 모델 정보 반환"""
    global _whisper_model

    return {
        "model_name": WHISPER_MODEL,
        "device": WHISPER_DEVICE,
        "engine": "faster-whisper",
        "loaded": _whisper_model is not None,
        "cpu_threads": CPU_THREADS,
    }


def unload_model():
    """모델을 메모리에서 해제"""
    global _whisper_model

    if _whisper_model is not None:
        print("[STT] Faster-Whisper 모델 메모리 해제")
        del _whisper_model
        _whisper_model = None


def test_stt_service():
    """STT 서비스 테스트"""
    try:
        info = get_model_info()
        print(f"[STT] 모델 정보: {info}")
        return True

    except Exception as e:
        print(f"[STT] 테스트 실패: {e}")
        return False


if __name__ == "__main__":
    test_stt_service()