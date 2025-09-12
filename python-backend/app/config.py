# app/config.py  (clean minimal)
from pathlib import Path
import os

# 기본 경로
BASE_DIR = Path(__file__).resolve().parent.parent
CHUNK_ROOT = BASE_DIR / "_uploads" / "chunks"
CHUNK_ROOT.mkdir(parents=True, exist_ok=True)

# STT 관련 최소 설정
MODEL_NAME = os.getenv("WHISPER_MODEL", "medium")
CPU_THREADS = os.cpu_count() or 4
MAX_UPLOAD_BYTES = int(os.getenv("MAX_UPLOAD_BYTES", 5 * 1024 * 1024))  # 5MB 기본

# DB URL
DB_URL = os.getenv("DB_URL", "mysql+pymysql://root:1004@127.0.0.1:3306/plun")

# (하위 호환) settings 심볼을 기대하는 코드가 있을 수 있어 빈 객체 제공
class _Settings: ...
settings = _Settings()
