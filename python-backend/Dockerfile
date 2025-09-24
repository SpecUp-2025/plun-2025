FROM python:3.11-slim

RUN apt-get update \
 && apt-get install -y ffmpeg portaudio19-dev libportaudio2 libportaudiocpp0

COPY requirements.txt .
RUN pip install --no-cache-dir --index-url https://download.pytorch.org/whl/cpu \
    torch==2.1.0 torchaudio==2.1.0
RUN pip install --no-cache-dir -r requirements.txt

COPY main.py .

CMD ["python", "main.py"]
