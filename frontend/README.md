# 실시간 화상회의 + STT + AI 요약 시스템

WebRTC SFU 기반 화상회의와 실시간 음성인식, AI 요약이 통합된 온프레미스 솔루션입니다.

## 주요 기능

- **화상회의**: WebRTC SFU 기반 다자간 화상회의
- **실시간 녹음**: 8초 단위 청크로 실시간 오디오 스트리밍
- **음성인식**: Whisper 기반 온프레미스 STT
- **AI 요약**: Ollama 로컬 LLM으로 회의록 자동 생성
- **회의록 관리**: 전사, 요약, 액션 아이템 DB 저장

## 시스템 요구사항

### 하드웨어
- **CPU**: 4코어 이상 (Whisper STT용)
- **메모리**: 8GB 이상 (Whisper Medium 모델 ~5GB 사용)
- **디스크**: 10GB 이상 여유 공간
- **GPU**: 선택사항 (CUDA 지원 시 STT 속도 5-10배 향상)

### 소프트웨어
- **OS**: Windows 10/11, Ubuntu 20.04+, macOS 12+
- **Python**: 3.8 이상
- **Node.js**: 16 이상 (프론트엔드)
- **FFmpeg**: 오디오 처리용

## 설치 및 설정

### 1. Python 백엔드 설치

```powershell
# 가상환경 생성
python -m venv meeting-env
meeting-env\Scripts\activate  # Windows
# source meeting-env/bin/activate  # Linux/Mac

# 기본 패키지 업그레이드
python -m pip install -U pip setuptools wheel

# PyTorch 설치 (CPU 버전)
pip install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cpu

# GPU 버전 (CUDA 11.8)
# pip install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cu118

# 핵심 의존성 설치
pip install openai-whisper fastapi uvicorn[standard] python-multipart
pip install pydantic==2.8.2 sqlalchemy requests pathlib
```

### 2. FFmpeg 설치

**Windows:**
```powershell
# Chocolatey 사용
choco install ffmpeg

# 또는 수동 설치 후 PATH 등록
# https://ffmpeg.org/download.html
```

**Ubuntu/Debian:**
```bash
sudo apt update
sudo apt install ffmpeg
```

**macOS:**
```bash
brew install ffmpeg
```

### 3. Ollama 설치 (AI 요약용)

```bash
# Linux/Mac
curl -fsSL https://ollama.ai/install.sh | sh

# Windows
# https://ollama.ai/download/windows 에서 다운로드
```

**모델 설치:**
```bash
# 한국어 지원 모델 설치 (권장) 이게 지금 사용중
ollama pull llama3.1:8b

# 또는 더 빠른 소형 모델
ollama pull qwen2:7b
```

### 4. 환경 설정

`app/config.py` 파일 생성:

```python
from pathlib import Path

# 오디오 파일 저장 경로
AUDIO_DIR = Path("./audio_files")
AUDIO_DIR.mkdir(exist_ok=True)

# Whisper STT 설정
WHISPER_MODEL = "medium"  # tiny, base, small, medium, large
WHISPER_DEVICE = "cpu"    # 또는 "cuda"
CPU_THREADS = 4           # CPU 코어 수에 맞게 조정

# Ollama AI 설정
OLLAMA_HOST = "http://localhost:11434"
OLLAMA_MODEL = "llama3.1:8b"  # 설치한 모델명

# 데이터베이스 설정
DATABASE_URL = "sqlite:///./meeting.db"  # 또는 PostgreSQL URL
```

### 5. 서버 실행

```bash
# 백엔드 서버 시작
cd python-backend
python main.py

# 로컬테스트용 필요시
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload

# 프론트엔드 개발 서버 (별도 터미널)
cd frontend
npm install
npm run dev
```

## API 엔드포인트

### 녹음 관리
- `POST /stt/start-recording` - 녹음 세션 시작
- `POST /stt/upload-chunk` - 실시간 오디오 청크 업로드
- `POST /stt/stop-recording` - 녹음 종료 및 처리 시작

### STT 및 요약
- `GET /stt/transcript/{room_no}` - 전사 텍스트 조회
- `POST /summary/generate` - AI 요약 생성
- `GET /summary/{room_no}` - 저장된 요약 조회

### 시스템 상태
- `GET /stt/model/info` - Whisper 모델 정보
- `GET /summary/provider/info` - Ollama 상태 확인

## 아키텍처 개요

```
[Vue.js 프론트엔드]
     ↓ WebRTC + HTTP API
[FastAPI 백엔드]
     ├── WebRTC SFU (화상회의)
     ├── 실시간 오디오 스트리밍
     ├── Whisper STT 엔진
     ├── Ollama AI 요약
     └── SQLite/PostgreSQL DB
```

## 주요 처리 플로우

1. **화상회의 시작** → SFU 서버 연결 + 미디어 스트림 교환
2. **녹음 시작** → 8초 단위 오디오 청크 실시간 업로드
3. **녹음 종료** → FFmpeg 병합 → Whisper STT → Ollama 요약 → DB 저장
4. **회의록 조회** → 전사/요약 데이터 반환

## 성능 최적화

### Whisper STT 최적화
```python
# CPU 스레드 수 조정
CPU_THREADS = 4  # CPU 코어 수에 맞게

# 모델 선택 (정확도 vs 속도)
WHISPER_MODEL = "medium"  # 권장 균형점
```

### Ollama 최적화
```bash
# GPU 메모리 설정
OLLAMA_GPU_MEMORY_FRACTION=0.8

# 동시 처리 수 제한
OLLAMA_MAX_LOADED_MODELS=1
```

## 트러블슈팅

### Whisper 관련
- **메모리 부족**: 모델을 "small"이나 "base"로 변경
- **처리 속도 느림**: GPU 사용 또는 CPU 스레드 수 증가

### Ollama 관련
- **연결 실패**: `ollama serve` 실행 확인
- **모델 없음**: `ollama pull llama3.1:8b` 실행

### FFmpeg 관련
- **명령어 없음**: PATH 환경변수 확인
- **코덱 오류**: FFmpeg 전체 버전 설치

## 보안 고려사항

- 모든 처리가 온프레미스에서 수행 (외부 API 없음)
- 오디오 파일 자동 정리 (처리 완료 후 삭제)
- HTTPS 사용 권장 (WebRTC 요구사항)