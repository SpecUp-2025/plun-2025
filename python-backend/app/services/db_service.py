# app/services/db_service.py
import pymysql
from sqlalchemy import create_engine, text
from sqlalchemy.exc import SQLAlchemyError
from typing import Optional, Tuple
from datetime import datetime
from app.config import DB_URL

# SQLAlchemy 엔진 생성
engine = create_engine(
    DB_URL,
    pool_size=10,
    max_overflow=20,
    pool_pre_ping=True,
    pool_recycle=3600
)

def db_ping() -> str:
    """데이터베이스 연결 테스트"""
    try:
        with engine.connect() as conn:
            result = conn.execute(text("SELECT VERSION()"))
            version = result.scalar()
            print(f"[DB] 연결 성공: MySQL {version}")
            return str(version)
    except Exception as e:
        print(f"[DB] 연결 실패: {e}")
        raise e

def save_transcript(room_no: int, transcript_text: str) -> bool:
    """전사 텍스트를 tb_meeting_transcript에 저장/업데이트"""
    try:
        with engine.begin() as conn:
            # UPSERT 쿼리 (MySQL ON DUPLICATE KEY UPDATE)
            query = text("""
                INSERT INTO tb_meeting_transcript (room_no, merged_text, updated_at)
                VALUES (:room_no, :merged_text, NOW())
                ON DUPLICATE KEY UPDATE
                merged_text = VALUES(merged_text),
                updated_at = NOW()
            """)
            
            conn.execute(query, {
                "room_no": room_no,
                "merged_text": transcript_text
            })
            
            print(f"[DB] 전사 텍스트 저장 완료: room_no={room_no}, 길이={len(transcript_text)}자")
            return True
            
    except SQLAlchemyError as e:
        print(f"[DB] 전사 텍스트 저장 실패: {e}")
        return False

def get_transcript(room_no: int) -> Optional[str]:
    """전사 텍스트 조회"""
    try:
        with engine.connect() as conn:
            query = text("SELECT merged_text FROM tb_meeting_transcript WHERE room_no = :room_no")
            result = conn.execute(query, {"room_no": room_no})
            row = result.fetchone()
            
            if row and row[0]:
                print(f"[DB] 전사 텍스트 조회 성공: room_no={room_no}, 길이={len(row[0])}자")
                return str(row[0])
            else:
                print(f"[DB] 전사 텍스트 없음: room_no={room_no}")
                return None
                
    except SQLAlchemyError as e:
        print(f"[DB] 전사 텍스트 조회 실패: {e}")
        return None

def save_summary(room_no: int, summary: str, action_items: str, decisions: str) -> bool:
    """요약을 tb_meeting_summary에 저장/업데이트"""
    try:
        with engine.begin() as conn:
            # UPSERT 쿼리
            query = text("""
                INSERT INTO tb_meeting_summary (room_no, summary, action_items, decisions, updated_at)
                VALUES (:room_no, :summary, :action_items, :decisions, NOW())
                ON DUPLICATE KEY UPDATE
                summary = VALUES(summary),
                action_items = VALUES(action_items),
                decisions = VALUES(decisions),
                updated_at = NOW()
            """)
            
            conn.execute(query, {
                "room_no": room_no,
                "summary": summary,
                "action_items": action_items,
                "decisions": decisions
            })
            
            print(f"[DB] 회의 요약 저장 완료: room_no={room_no}")
            return True
            
    except SQLAlchemyError as e:
        print(f"[DB] 회의 요약 저장 실패: {e}")
        return False

def update_calendar_contents(room_no: int, contents: str) -> bool:
    """달력 상세 내용 업데이트 (회의방 -> 달력 연결)"""
    try:
        with engine.begin() as conn:
            # 회의방의 cal_detail_no 조회
            query = text("SELECT cal_detail_no FROM tb_meeting_room WHERE room_no = :room_no")
            result = conn.execute(query, {"room_no": room_no})
            row = result.fetchone()
            
            if not row or not row[0]:
                print(f"[DB] 달력 연결 정보 없음: room_no={room_no}")
                return False
                
            cal_detail_no = row[0]
            
            # 달력 상세 내용 업데이트
            update_query = text("""
                UPDATE tb_calendar_detail 
                SET contents = :contents,
                    update_dt = NOW()
                WHERE cal_detail_no = :cal_detail_no
            """)
            
            conn.execute(update_query, {
                "contents": contents,
                "cal_detail_no": cal_detail_no
            })
            
            print(f"[DB] 달력 내용 업데이트 완료: cal_detail_no={cal_detail_no}")
            return True
            
    except SQLAlchemyError as e:
        print(f"[DB] 달력 내용 업데이트 실패: {e}")
        return False

def get_meeting_info(room_no: int) -> Optional[dict]:
    """회의 정보 조회"""
    try:
        with engine.connect() as conn:
            query = text("""
                SELECT mr.room_no, mr.title, mr.room_code, mr.cal_detail_no,
                       cd.title as calendar_title
                FROM tb_meeting_room mr
                LEFT JOIN tb_calendar_detail cd ON mr.cal_detail_no = cd.cal_detail_no
                WHERE mr.room_no = :room_no
            """)
            
            result = conn.execute(query, {"room_no": room_no})
            row = result.fetchone()
            
            if row:
                return {
                    "room_no": row[0],
                    "title": row[1],
                    "room_code": row[2],
                    "cal_detail_no": row[3],
                    "calendar_title": row[4]
                }
            else:
                print(f"[DB] 회의 정보 없음: room_no={room_no}")
                return None
                
    except SQLAlchemyError as e:
        print(f"[DB] 회의 정보 조회 실패: {e}")
        return None

# 별칭 함수들 (기존 코드와의 호환성)
def get_merged_transcript(room_no: int) -> Optional[str]:
    """get_transcript의 별칭"""
    return get_transcript(room_no)

def upsert_meeting_summary(room_no: int, full_summary: str, action_items: str, decisions: str) -> bool:
    """save_summary의 별칭"""
    return save_summary(room_no, full_summary, action_items, decisions)

# 테스트 함수
def test_db_connection():
    """데이터베이스 연결 테스트"""
    try:
        version = db_ping()
        print(f"[DB] 테스트 성공: {version}")
        return True
    except Exception as e:
        print(f"[DB] 테스트 실패: {e}")
        return False

if __name__ == "__main__":
    test_db_connection()