# app/services/db_service.py (요약 함수 제거 후)
from sqlalchemy import create_engine, text
from sqlalchemy.engine import Engine
from typing import Optional, Tuple
from app.config import DB_URL

engine: Engine = create_engine(DB_URL, pool_pre_ping=True, future=True)

def db_ping() -> str:
    with engine.connect() as conn:
        return conn.execute(text("SELECT VERSION()")).scalar_one()

def get_next_session_no(room_no: int) -> int:
    with engine.begin() as conn:
        val = conn.execute(
            text("SELECT COALESCE(MAX(session_no),0)+1 AS next_no "
                 "FROM tb_meeting_transcript_seg WHERE room_no=:r"),
            {"r": room_no}
        ).scalar_one()
        return int(val)

def upsert_segment_and_append(
    room_no: int,
    session_no: int,
    part_no: int,
    text_chunk: str,
    start_ms: Optional[int],
    end_ms: Optional[int],
    sep: str = "\n\n"
) -> Tuple[bool, int]:
    with engine.begin() as conn:
        existing = conn.execute(
            text("SELECT seg_no FROM tb_meeting_transcript_seg "
                 "WHERE room_no=:r AND session_no=:s AND part_no=:p"),
            {"r": room_no, "s": session_no, "p": part_no}
        ).scalar_one_or_none()

        if existing is None:
            conn.execute(
                text("INSERT INTO tb_meeting_transcript_seg "
                     "(room_no, session_no, part_no, text, start_ms, end_ms) "
                     "VALUES (:r,:s,:p,:t,:a,:b)"),
                {"r": room_no, "s": session_no, "p": part_no, "t": text_chunk, "a": start_ms, "b": end_ms}
            )
            conn.execute(
                text("""
                    INSERT INTO tb_meeting_transcript (room_no, merged_text)
                    VALUES (:r, :t)
                    ON DUPLICATE KEY UPDATE
                      merged_text = CASE
                          WHEN COALESCE(merged_text, '') = '' THEN :t
                          ELSE CONCAT(merged_text, :sep, :t)
                      END,
                      updated_at = CURRENT_TIMESTAMP
                """),
                {"r": room_no, "t": text_chunk, "sep": sep}
            )
            return True, 1
        else:
            conn.execute(
                text("UPDATE tb_meeting_transcript_seg "
                     "SET text=:t, start_ms=:a, end_ms=:b "
                     "WHERE seg_no=:id"),
                {"t": text_chunk, "a": start_ms, "b": end_ms, "id": existing}
            )
            return False, 0

def append_merged_transcript(room_no: int, new_text: str, sep: str = "\n"):
    sql = text("""
        INSERT INTO plun.tb_meeting_transcript (room_no, merged_text)
        VALUES (:room_no, :text)
        ON DUPLICATE KEY UPDATE
          merged_text = CASE
              WHEN COALESCE(merged_text, '') = '' THEN :text
              ELSE CONCAT(merged_text, :sep, :text)
          END,
          updated_at = CURRENT_TIMESTAMP
    """)
    with engine.begin() as conn:
        conn.execute(sql, {"room_no": room_no, "text": new_text, "sep": sep})

def get_merged_transcript(room_no: int) -> str | None:
    sql = text("""
        SELECT merged_text
        FROM plun.tb_meeting_transcript
        WHERE room_no = :room_no
        LIMIT 1
    """)
    with engine.connect() as conn:
        row = conn.execute(sql, {"room_no": room_no}).fetchone()
        return row[0] if row and row[0] is not None else None
    
def upsert_meeting_summary(room_no: int, summary: str, action_items_md: str, decisions_md: str) -> None:
    sql = text("""
        INSERT INTO plun.tb_meeting_summary (room_no, summary, action_items, decisions)
        VALUES (:room_no, :summary, :ai, :dc)
        ON DUPLICATE KEY UPDATE
          summary      = :summary,
          action_items = :ai,
          decisions    = :dc,
          updated_at   = CURRENT_TIMESTAMP
    """)
    with engine.begin() as conn:
        conn.execute(sql, {
            "room_no": room_no,
            "summary": summary,
            "ai": action_items_md,
            "dc": decisions_md,
        })
