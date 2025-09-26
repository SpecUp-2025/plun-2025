FROM python:3.11-slim

RUN apt-get update && apt-get install -y \
    ffmpeg \
    libportaudio2 portaudio19-dev libportaudiocpp0 \
    libsndfile1 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir --index-url https://download.pytorch.org/whl/cpu \
    torch==2.1.0 torchaudio==2.1.0
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

CMD ["python", "-u", "main.py"]
