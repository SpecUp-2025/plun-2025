# app/config.py
import os
from pathlib import Path
from dotenv import load_dotenv

# .env 파일 로드
load_dotenv()

# 기본 경로 설정
BASE_DIR = Path(__file__).resolve().parent.parent
AUDIO_DIR = BASE_DIR / "audio_chunks"
AUDIO_DIR.mkdir(parents=True, exist_ok=True)

# 데이터베이스 설정
DB_HOST = os.getenv("DB_HOST", "127.0.0.1")
# DB_HOST = os.getenv("DB_HOST", "172.30.1.15")
DB_PORT = os.getenv("DB_PORT", "3306")
DB_USER = os.getenv("DB_USER", "root")
DB_PASSWORD = os.getenv("DB_PASSWORD", "1004")
DB_NAME = os.getenv("DB_NAME", "plun")
DB_URL = f"mysql+pymysql://{DB_USER}:{DB_PASSWORD}@{DB_HOST}:{DB_PORT}/{DB_NAME}"

# Whisper STT 설정
WHISPER_MODEL = os.getenv("WHISPER_MODEL", "medium")  # tiny, base, small, medium, large
WHISPER_DEVICE = os.getenv("WHISPER_DEVICE", "cpu")   # cpu 또는 cuda
CPU_THREADS = int(os.getenv("CPU_THREADS", str(os.cpu_count() or 4)))

# AI 요약 설정 (OpenAI 또는 Ollama)
AI_PROVIDER = "ollama"  # 더 이상 사용 안 함 (ollama 고정)
# OLLAMA_HOST = "http://localhost:11434"
OLLAMA_HOST = os.getenv("OLLAMA_HOST","http://127.0.0.1:11434")
OLLAMA_MODEL = "llama3.2:3b"  # 또는 "qwen2.5:7b"

# 파일 업로드 제한
MAX_UPLOAD_BYTES = int(os.getenv("MAX_UPLOAD_BYTES", 10 * 1024 * 1024))  # 10MB

# SPRING
SPRING_API_BASE_URL = os.getenv("SPRING_API_BASE_URL", "http://localhost:8080")

# 로그 설정
LOG_LEVEL = os.getenv("LOG_LEVEL", "INFO")

print(f"[CONFIG] 설정 로드 완료:")
print(f"[CONFIG] - DB: {DB_HOST}:{DB_PORT}/{DB_NAME}")
print(f"[CONFIG] - Whisper 모델: {WHISPER_MODEL}")
print(f"[CONFIG] - AI 제공자: {AI_PROVIDER}")
print(f"[CONFIG] - 오디오 저장 경로: {AUDIO_DIR}")