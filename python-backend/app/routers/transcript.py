from fastapi import APIRouter, Query, Body, HTTPException
from pydantic import BaseModel, Field
from typing import Optional
from app.services.db_service import db_ping, get_next_session_no, upsert_segment_and_append

router = APIRouter()

@router.get("/db/ping")
def ping():
    v = db_ping()
    return {"ok": True, "version": v}

@router.post("/stt/session/start")
def start_session(room_no: int = Query(..., ge=1)):
    next_no = get_next_session_no(room_no)
    return {"ok": True, "room_no": room_no, "session_no": next_no}

class SegmentIn(BaseModel):
    room_no: int = Field(..., ge=1)
    session_no: int = Field(..., ge=1)
    part_no: int = Field(..., ge=0)
    text: str = Field(..., min_length=1)
    start_ms: Optional[int] = Field(None, ge=0)
    end_ms: Optional[int] = Field(None, ge=0)

@router.post("/stt/segment/append")
def append_segment(payload: SegmentIn = Body(...)):
    inserted, _ = upsert_segment_and_append(
        room_no=payload.room_no,
        session_no=payload.session_no,
        part_no=payload.part_no,
        text_chunk=payload.text,
        start_ms=payload.start_ms,
        end_ms=payload.end_ms,
        sep="\n\n"
    )
    return {"ok": True, "inserted": inserted}
