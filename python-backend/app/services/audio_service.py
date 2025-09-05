import subprocess, tempfile, shutil, time
from pathlib import Path
from typing import List
from fastapi import HTTPException

def which_ffmpeg() -> str:
    ff = shutil.which("ffmpeg")
    if not ff:
        raise HTTPException(500, "ffmpeg not found in PATH")
    return ff

def preconvert_webm_to_wav(src_webm: Path, out_wav: Path) -> bool:
    ffmpeg = which_ffmpeg()
    out_wav.parent.mkdir(parents=True, exist_ok=True)
    cmd = [ffmpeg, "-y", "-hide_banner", "-i", str(src_webm), "-vn", "-map", "0:a:0",
           "-ac", "1", "-ar", "16000", "-c:a", "pcm_s16le", str(out_wav)]
    proc = subprocess.run(cmd, capture_output=True, text=True)
    return proc.returncode == 0 and out_wav.exists() and out_wav.stat().st_size > 0

def merge_wavs_in_order(parts: List[Path], out_path: Path) -> float:
    if not parts: raise HTTPException(400, "no wav parts available")
    ffmpeg = which_ffmpeg()
    tmpdir = Path(tempfile.mkdtemp(prefix="merge_", dir=str(out_path.parent)))
    list_txt = tmpdir / "list.txt"
    with open(list_txt, "w", encoding="utf-8") as f:
        for p in parts: f.write(f"file '{p.as_posix()}'\n")
    t0 = time.time()
    cmd = [ffmpeg, "-y", "-hide_banner", "-f", "concat", "-safe", "0", "-i", str(list_txt),
           "-c", "copy", str(out_path)]
    subprocess.run(cmd, check=True, capture_output=True)
    return time.time() - t0
