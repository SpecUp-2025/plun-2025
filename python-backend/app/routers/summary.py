# app/routers/summary.py
from typing import Optional
from fastapi import APIRouter, HTTPException, Query
from pydantic import BaseModel

from app.services.db_service import get_merged_transcript, upsert_meeting_summary
from app.services.summary_llm import generate_minutes_md
from app.services.summary_parse import extract_section

router = APIRouter()

class SummaryOut(BaseModel):
    room_no: int
    provider: str
    minutes_md: str
    saved: bool

def _fallback_md(text: str, limit: int = 600) -> str:
    head = " ".join((text or "").split())[:limit]
    return f"""# 회의 요약
- {head}

## 액션 아이템
- (없음)

## 결정 사항
- (없음)
"""

def _do_generate(room_no: int, limit_chars: Optional[int], save: bool) -> SummaryOut:
    # 1) 전사 로드
    text = get_merged_transcript(room_no)
    if not text or not text.strip():
        raise HTTPException(404, "전사 텍스트 없음")
    src = text[:limit_chars] if limit_chars else text

    # 2) 생성 (실패시 폴백)
    provider = "ollama"
    try:
        md = generate_minutes_md(src)
    except Exception:
        provider = "fallback"
        md = _fallback_md(src)

    # 3) 섹션 파싱 → DB 저장용 분리
    ai_md = extract_section(md, "액션 아이템")
    dc_md = extract_section(md, "결정 사항")

    saved = False
    if save:
        try:
            upsert_meeting_summary(room_no, md, ai_md, dc_md)
            saved = True
        except Exception:
            saved = False  # 저장 실패해도 API는 200으로 본문 반환

    return SummaryOut(room_no=room_no, provider=provider, minutes_md=md, saved=saved)

@router.post("/summary/generate", response_model=SummaryOut)
def summary_generate(
    room_no: int = Query(..., description="전사 텍스트가 있는 room_no"),
    limit_chars: Optional[int] = Query(1200, ge=300, le=5000, description="요약 입력 길이"),
    save: bool = Query(True, description="DB 저장 여부"),
):
    return _do_generate(room_no, limit_chars, save)

# ✅ 이전 호환 (기존 프론트가 /minutes/generate를 쓰고 있어도 깨지지 않게)
@router.post("/minutes/generate", response_model=SummaryOut)
def minutes_generate_compat(
    room_no: int = Query(...),
    limit_chars: Optional[int] = Query(1200, ge=300, le=5000),
    save: bool = Query(True),
):
    return _do_generate(room_no, limit_chars, save)
