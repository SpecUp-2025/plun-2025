# python-backend (Whisper single-file verify)

## 준비
- Windows + Python 3.8+
- (권장) 가상환경
- PyTorch (CPU or CUDA), openai-whisper 설치
- ffmpeg 설치 (PATH 등록)

## 설치 (요약)
```powershell
python -m venv whisper-env
whisper-env\Scripts\activate
python -m pip install -U pip setuptools wheel
pip install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cpu
pip install -U openai-whisper fastapi uvicorn[standard] python-multipart pydantic==2.8.2
