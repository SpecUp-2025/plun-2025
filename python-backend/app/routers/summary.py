# app/routers/summary.py
from fastapi import APIRouter, Query, HTTPException, BackgroundTasks
from pydantic import BaseModel
from typing import Optional

from app.services.db_service import (
    get_transcript,
    save_summary,
    update_calendar_contents,
)
from app.services.ai_service import generate_summary, get_ai_provider_info

router = APIRouter()


class SummaryResponse(BaseModel):
    room_no: int
    summary: str
    action_items: str
    decisions: str
    provider: str
    success: bool


class SummaryRequest(BaseModel):
    room_no: int
    save_to_db: bool = True
    update_calendar: bool = True


@router.post("/summary/generate", response_model=SummaryResponse)
async def generate_meeting_summary(request: SummaryRequest):
    """회의 요약 생성 및 저장"""
    try:
        room_no = request.room_no
        save_to_db = request.save_to_db
        update_calendar = request.update_calendar

        print(f"[SUMMARY] 요약 생성 요청: room_no={room_no}")

        # 1. 전사 텍스트 조회
        transcript = get_transcript(room_no)
        if not transcript:
            raise HTTPException(
                status_code=404, detail="전사 텍스트를 찾을 수 없습니다."
            )

        print(f"[SUMMARY] 전사 텍스트 조회 완료: {len(transcript)}자")

        # 2. AI 요약 생성
        summary, action_items, decisions = generate_summary(transcript)

        provider_info = get_ai_provider_info()
        provider = provider_info.get("provider", "unknown")

        print(f"[SUMMARY] 요약 생성 완료 ({provider})")

        # 3. DB 저장 (옵션)
        if save_to_db:
            db_saved = save_summary(room_no, summary, action_items, decisions)
            if db_saved:
                print(f"[SUMMARY] 요약 DB 저장 완료")
            else:
                print(f"[SUMMARY] 요약 DB 저장 실패")

        # 4. 달력 업데이트 (옵션)
        if update_calendar:
            calendar_updated = update_calendar_contents(room_no, summary)
            if calendar_updated:
                print(f"[SUMMARY] 달력 업데이트 완료")
            else:
                print(f"[SUMMARY] 달력 업데이트 실패")

        return SummaryResponse(
            room_no=room_no,
            summary=summary,
            action_items=action_items,
            decisions=decisions,
            provider=provider,
            success=True,
        )

    except HTTPException:
        raise
    except Exception as e:
        print(f"[SUMMARY] 요약 생성 실패: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/summary/generate/{room_no}", response_model=SummaryResponse)
async def generate_summary_by_room_no(
    room_no: int,
    save: bool = Query(True, description="DB 저장 여부"),
    update_calendar: bool = Query(True, description="달력 업데이트 여부"),
):
    """회의실 번호로 요약 생성 (GET 방식)"""
    request = SummaryRequest(
        room_no=room_no, save_to_db=save, update_calendar=update_calendar
    )
    return await generate_meeting_summary(request)


@router.get("/summary/{room_no}")
def get_existing_summary(room_no: int):
    """기존 저장된 요약 조회"""
    try:
        from app.services.db_service import engine
        from sqlalchemy import text

        with engine.connect() as conn:
            query = text(
                """
                SELECT summary, action_items, decisions, updated_at
                FROM TB_MEETING_SUMMARY 
                WHERE room_no = :room_no
            """
            )
            result = conn.execute(query, {"room_no": room_no})
            row = result.fetchone()

            if row:
                return {
                    "room_no": room_no,
                    "summary": row[0],
                    "action_items": row[1],
                    "decisions": row[2],
                    "updated_at": row[3].isoformat() if row[3] else None,
                    "found": True,
                }
            else:
                return {
                    "room_no": room_no,
                    "found": False,
                    "message": "저장된 요약을 찾을 수 없습니다.",
                }

    except Exception as e:
        print(f"[SUMMARY] 기존 요약 조회 실패: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/summary/provider/info")
def get_summary_provider_info():
    """AI 제공자 정보 조회"""
    try:
        provider_info = get_ai_provider_info()
        return {"success": True, "provider_info": provider_info}
    except Exception as e:
        print(f"[SUMMARY] 제공자 정보 조회 실패: {e}")
        raise HTTPException(status_code=500, detail=str(e))


# 하위 호환성을 위한 별칭 엔드포인트
@router.post("/minutes/generate", response_model=SummaryResponse)
async def generate_minutes_compat(request: SummaryRequest):
    """기존 코드 호환용 별칭"""
    return await generate_meeting_summary(request)


@router.get("/minutes/generate/{room_no}", response_model=SummaryResponse)
async def generate_minutes_by_room_compat(
    room_no: int, save: bool = Query(True), update_calendar: bool = Query(True)
):
    """기존 코드 호환용 별칭"""
    return await generate_summary_by_room_no(room_no, save, update_calendar)
