# app/services/stt_service.py
import os
import time
import torch
import whisper
from pathlib import Path
from typing import Optional, Tuple
from app.config import WHISPER_MODEL, WHISPER_DEVICE, CPU_THREADS

# CPU 성능 최적화 설정
os.environ.setdefault("OMP_NUM_THREADS", str(CPU_THREADS))
os.environ.setdefault("MKL_NUM_THREADS", str(CPU_THREADS))

try:
    torch.set_num_threads(CPU_THREADS)
    torch.set_num_interop_threads(min(4, CPU_THREADS))
except:
    pass

# 전역 모델 인스턴스 (지연 로딩)
_whisper_model = None


def load_whisper_model():
    """Whisper 모델 지연 로딩"""
    global _whisper_model

    if _whisper_model is None:
        print(f"[STT] Whisper 모델 로딩 시작: {WHISPER_MODEL}")
        start_time = time.time()

        try:
            _whisper_model = whisper.load_model(
                name=WHISPER_MODEL, device=WHISPER_DEVICE
            )

            load_time = time.time() - start_time
            print(f"[STT] Whisper 모델 로딩 완료: {load_time:.2f}초")

        except Exception as e:
            print(f"[STT] Whisper 모델 로딩 실패: {e}")
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

        # Whisper 최적화 설정
        options = {
            "language": language,
            "fp16": False,  # CPU 안정성을 위해 비활성화
            "verbose": False,
            "temperature": 0.0,  # 일관된 결과를 위한 결정적 출력
            "compression_ratio_threshold": 2.4,
            "logprob_threshold": -1.0,
            "no_speech_threshold": 0.6,
        }

        # 전사 실행
        result = model.transcribe(str(audio_path), **options)
        processing_time = time.time() - start_time

        # 결과 처리
        text = (result.get("text", "") or "").strip()
        detected_language = result.get("language", "unknown")

        print(f"[STT] 전사 완료: {processing_time:.2f}초, {len(text)}자")
        print(f"[STT] 감지 언어: {detected_language}")
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

        options = {
            "language": language,
            "fp16": False,
            "verbose": False,
            "word_timestamps": True,  # 단어별 타임스탬프 활성화
        }

        result = model.transcribe(str(audio_path), **options)

        # 세그먼트 정보 추출
        segments = []
        for segment in result.get("segments", []):
            segments.append(
                {
                    "start": segment.get("start", 0),
                    "end": segment.get("end", 0),
                    "text": segment.get("text", "").strip(),
                }
            )

        print(f"[STT] 세그먼트별 전사 완료: {len(segments)}개 세그먼트")

        return {
            "text": result.get("text", "").strip(),
            "language": result.get("language", "unknown"),
            "segments": segments,
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
        "loaded": _whisper_model is not None,
        "cpu_threads": CPU_THREADS,
    }


def unload_model():
    """모델을 메모리에서 해제"""
    global _whisper_model

    if _whisper_model is not None:
        print("[STT] Whisper 모델 메모리 해제")
        del _whisper_model
        _whisper_model = None

        # GPU 메모리 정리 (CUDA 사용 시)
        if torch.cuda.is_available():
            torch.cuda.empty_cache()


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
