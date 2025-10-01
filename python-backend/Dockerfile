FROM python:3.9-slim

RUN apt-get update && apt-get install -y \
    ffmpeg \
    libportaudio2 portaudio19-dev libportaudiocpp0 \
    libsndfile1 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Whisper 먼저 (PyTorch 자동 설치)
RUN pip install -U openai-whisper

# requirements.txt (torch/whisper 제외)
COPY requirements.txt .
RUN pip install -r requirements.txt

COPY . .
CMD ["python", "-u", "main.py"]