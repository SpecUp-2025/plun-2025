from pathlib import Path
import os

BASE_DIR = Path(__file__).resolve().parent.parent
CHUNK_ROOT = BASE_DIR / "_uploads" / "chunks"
CHUNK_ROOT.mkdir(parents=True, exist_ok=True)

MODEL_NAME = os.getenv("WHISPER_MODEL", "small")

CPU_THREADS = os.cpu_count() or 4
