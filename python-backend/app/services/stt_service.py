import os, time
from pathlib import Path
from typing import Tuple
import whisper
import torch
from app.config import MODEL_NAME, CPU_THREADS

os.environ.setdefault("OMP_NUM_THREADS", str(CPU_THREADS))
os.environ.setdefault("MKL_NUM_THREADS", str(CPU_THREADS))
try:
    torch.set_num_threads(CPU_THREADS)
    torch.set_num_interop_threads(min(4, CPU_THREADS))
except:
    pass

_MODEL = None

def get_model():
    global _MODEL
    if _MODEL is None:
        _MODEL = whisper.load_model(MODEL_NAME, device="cpu")
    return _MODEL

def transcribe_ko(wav_path: Path) -> Tuple[str, float]:
    m = get_model()
    t0 = time.time()
    r = m.transcribe(str(wav_path), fp16=False)
    return (r.get("text") or "").strip(), time.time() - t0
