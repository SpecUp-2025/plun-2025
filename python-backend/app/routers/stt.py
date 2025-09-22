# app/routers/stt.py
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import Optional

from app.services.db_service import get_transcript
from app.services.stt_service import get_model_info

router = APIRouter()


class TranscriptResponse(BaseModel):
    room_no: int
    transcript: Optional[str]
    length: int
    success: bool


@router.get("/stt/transcript/{room_no}", response_model=TranscriptResponse)
def get_transcript_by_room(room_no: int):
    """회의실 번호로 전사 텍스트 조회"""
    try:
        transcript = get_transcript(room_no)

        return TranscriptResponse(
            room_no=room_no,
            transcript=transcript,
            length=len(transcript) if transcript else 0,
            success=transcript is not None,
        )

    except Exception as e:
        print(f"[STT] 전사 조회 실패: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/stt/model/info")
def get_stt_model_info():
    """STT 모델 정보 및 상태 조회"""
    try:
        model_info = get_model_info()
        return {"success": True, "model_info": model_info}
    except Exception as e:
        print(f"[STT] 모델 정보 조회 실패: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/stt/model/unload")
def unload_stt_model():
    """STT 모델 메모리에서 해제 (메모리 절약용)"""
    try:
        from app.services.stt_service import unload_model

        unload_model()

        return {"success": True, "message": "STT 모델이 메모리에서 해제되었습니다."}
    except Exception as e:
        print(f"[STT] 모델 해제 실패: {e}")
        raise HTTPException(status_code=500, detail=str(e))
