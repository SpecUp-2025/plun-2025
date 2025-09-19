# main.py - FastAPI 서버 진입점
import os
import sys
from pathlib import Path
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
import uvicorn

# 경로 설정
BASE_DIR = Path(__file__).resolve().parent
sys.path.append(str(BASE_DIR))

# 라우터 및 설정 import
from app.routers.recording import router as recording_router
from app.config import AUDIO_DIR

# FastAPI 앱 생성
app = FastAPI(
    title="Meeting STT & AI Summary Server",
    description="화상회의 녹음, STT 전사, AI 요약 서비스",
    version="2.0.0",
)

# CORS 설정 (개발용 - 프로덕션에서는 제한 필요)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 라우터 등록
app.include_router(recording_router, tags=["Recording"])


@app.on_event("startup")
async def startup_event():
    """서버 시작 시 초기화 작업"""
    print(f"[MAIN] Meeting STT & AI Summary Server v2.0.0 시작")
    print(f"[MAIN] 오디오 저장 경로: {AUDIO_DIR}")

    # 필수 디렉토리 생성
    AUDIO_DIR.mkdir(parents=True, exist_ok=True)

    # 데이터베이스 연결 테스트
    try:
        from app.services.db_service import test_db_connection

        test_db_connection()
        print(f"[MAIN] 데이터베이스 연결: OK")
    except Exception as e:
        print(f"[MAIN] 데이터베이스 연결 실패: {e}")
        print(f"[MAIN] STT 기능은 작동하지만 DB 저장 불가")

    print(f"[MAIN] 서버 초기화 완료")


@app.get("/")
def root():
    """루트 엔드포인트 - API 정보 제공"""
    return {
        "service": "Meeting STT & AI Summary Server",
        "version": "2.0.0",
        "status": "running",
        "endpoints": {
            "recording": "/stt/",
            "health": "/health",
            "sessions": "/stt/sessions",
            "docs": "/docs",
        },
    }


@app.get("/health")
def health_check():
    """시스템 상태 확인"""
    try:
        from app.services.db_service import db_ping

        db_version = db_ping()

        return {
            "status": "healthy",
            "services": {
                "database": {"status": "connected", "version": db_version},
                "stt": {"status": "ready", "engine": "whisper"},
            },
        }
    except Exception as e:
        return {
            "status": "partial",
            "error": str(e),
            "note": "STT 작동하지만 DB 연결 문제",
        }


@app.get("/info")
def server_info():
    """서버 상세 정보"""
    return {
        "server": {
            "name": "Meeting STT & AI Summary Server",
            "version": "2.0.0",
            "audio_storage": str(AUDIO_DIR),
        },
        "stt": {"engine": "OpenAI Whisper", "model": "medium"},
        "ai": {"provider": "Ollama", "model": "llama3.1:8b"},
    }


if __name__ == "__main__":
    print("서버 시작 중...")
    uvicorn.run(app, host="0.0.0.0", port=8000)
