# app/services/ai_service.py
import re
import requests
from typing import Tuple
from app.config import OLLAMA_HOST, OLLAMA_MODEL


def generate_summary_ollama(transcript_text: str) -> Tuple[str, str, str]:
    """Ollama 로컬 LLM을 사용한 회의록 요약"""
    print(f"[AI] Ollama 요약 시작 (모델: {OLLAMA_MODEL})")

    # 컨텍스트 길이 제한
    text_limit = 8000
    input_text = (
        transcript_text[:text_limit]
        if len(transcript_text) > text_limit
        else transcript_text
    )

    prompt = f"""회의 전사 내용을 자연스러운 한국어 회의록으로 작성하세요.

전사 내용:
\"\"\"{input_text}\"\"\"

작성 규칙:
1. 간결하고 읽기 쉬운 문장으로 작성
2. 불필요한 마크다운 기호(#, *, -, []) 최소화
3. 각 항목은 "- "로만 시작 (체크박스 없음)
4. 담당자와 기한이 명확하지 않으면 생략
5. 실제 회의 내용이 없으면 "없음" 표시

출력 형식:

회의 요약
- 핵심 내용 1
- 핵심 내용 2
- 핵심 내용 3

액션 아이템
- 작업 내용 (담당: 이름, 기한: 날짜)
- 작업 내용 (담당: 이름)

결정 사항
- 합의된 내용 1
- 합의된 내용 2

주의: 제목에 ## 같은 기호를 붙이지 마세요. 위 형식을 정확히 따르세요.
"""

    try:
        url = f"{OLLAMA_HOST.rstrip('/')}/api/generate"

        payload = {
            "model": OLLAMA_MODEL,
            "prompt": prompt,
            "stream": False,
            "options": {
                "temperature": 0.3,
                "num_predict": 600,
                "top_p": 0.9,
            },
        }

        response = requests.post(
            url,
            json=payload,
            headers={"Content-Type": "application/json"},
            timeout=(30, 1800),
        )

        response.raise_for_status()
        result = response.json()

        full_response = result.get("response", "").strip()

        if not full_response:
            raise ValueError("Ollama에서 빈 응답을 받았습니다")

        print(f"[AI] Ollama 요약 완료 (길이: {len(full_response)}자)")

        full_response = clean_markdown_response(full_response)
        summary, action_items, decisions = parse_summary_sections(full_response)

        return summary, action_items, decisions

    except requests.exceptions.RequestException as e:
        print(f"[AI] Ollama 연결 실패: {e}")
        raise e
    except Exception as e:
        print(f"[AI] Ollama 요약 실패: {e}")
        raise e


def clean_markdown_response(text: str) -> str:
    """LLM 응답에서 불필요한 요소 제거"""
    # 코드블록 제거
    text = re.sub(r"```[\w]*\n?", "", text)
    text = re.sub(r"\n?```", "", text)

    # ## 제목 기호 제거
    text = re.sub(r"^##\s+", "", text, flags=re.MULTILINE)

    # 체크박스 제거 (- [ ] → -)
    text = re.sub(r"-\s*\[\s*\]\s*", "- ", text)

    # 연속된 # 기호 제거
    text = re.sub(r"#{2,}", "", text)

    # 빈 줄 정리 (3개 이상 연속 → 2개로)
    text = re.sub(r"\n{3,}", "\n\n", text)

    return text.strip()


def parse_summary_sections(full_text: str) -> Tuple[str, str, str]:
    """구조화된 응답에서 섹션별 내용 추출"""
    summary = ""
    action_items = ""
    decisions = ""

    try:
        summary_match = re.search(
            r"(?:##\s*)?회의\s*요약\s*\n(.*?)(?=(?:##\s*)?액션|$)",
            full_text,
            re.DOTALL | re.IGNORECASE,
        )
        if summary_match:
            summary = summary_match.group(1).strip()

        action_match = re.search(
            r"(?:##\s*)?액션\s*아이템\s*\n(.*?)(?=(?:##\s*)?결정|$)",
            full_text,
            re.DOTALL | re.IGNORECASE,
        )
        if action_match:
            action_items = action_match.group(1).strip()

        decision_match = re.search(
            r"(?:##\s*)?결정\s*사항\s*\n(.*?)$", full_text, re.DOTALL | re.IGNORECASE
        )
        if decision_match:
            decisions = decision_match.group(1).strip()

        # 빈 섹션 처리
        if not summary:
            summary = "- 회의 요약 정보 없음"
        if not action_items:
            action_items = "- 액션 아이템 없음"
        if not decisions:
            decisions = "- 결정 사항 없음"

        print(
            f"[AI] 섹션 파싱 완료: 요약({len(summary)}자), 액션({len(action_items)}자), 결정({len(decisions)}자)"
        )

        return summary, action_items, decisions

    except Exception as e:
        print(f"[AI] 섹션 파싱 실패: {e}")
        return full_text[:500], "- 액션 아이템 파싱 실패", "- 결정 사항 파싱 실패"


def generate_summary(transcript_text: str) -> Tuple[str, str, str]:
    """
    전사 텍스트를 구조화된 요약으로 변환

    Args:
        transcript_text: 회의 전사 텍스트

    Returns:
        (전체_요약, 액션_아이템, 결정_사항) 튜플
    """
    print(f"[AI] AI 요약 생성 시작 (Ollama)")
    print(f"[AI] 입력 텍스트 길이: {len(transcript_text)}자")

    if not transcript_text or not transcript_text.strip():
        raise ValueError("전사 텍스트가 비어있습니다")

    try:
        summary, action_items, decisions = generate_summary_ollama(transcript_text)

        # 결과 검증
        if not summary and not action_items and not decisions:
            print(f"[AI] WARNING: 모든 섹션이 비어있습니다")

        print(f"[AI] 요약 생성 완료!")
        return summary, action_items, decisions

    except Exception as e:
        print(f"[AI] 요약 생성 실패: {type(e).__name__}: {e}")
        # 폴백: 간단한 요약 생성
        return generate_fallback_summary(transcript_text)


def generate_fallback_summary(transcript_text: str) -> Tuple[str, str, str]:
    """AI 요약 실패 시 폴백 요약 생성"""
    print(f"[AI] 폴백 요약 생성")

    # 텍스트 첫 부분을 요약으로 사용
    preview = transcript_text[:300].strip()
    if len(transcript_text) > 300:
        preview += "..."

    summary = f"- 회의 내용: {preview}"
    action_items = "- [ ] AI 요약 실패로 인한 수동 정리 필요"
    decisions = "- AI 요약 실패로 인한 수동 확인 필요"

    return summary, action_items, decisions


def get_ai_provider_info() -> dict:
    """현재 AI 제공자 정보 및 상태 반환"""
    info = {
        "provider": "ollama",
        "model": OLLAMA_MODEL,
        "host": OLLAMA_HOST,
        "available": False,
    }

    try:
        # Ollama 서버 연결 테스트
        response = requests.get(f"{OLLAMA_HOST}/api/tags", timeout=5)
        info["available"] = response.status_code == 200

        if info["available"]:
            # 사용 가능한 모델 목록 조회
            models = response.json().get("models", [])
            info["available_models"] = [model.get("name", "") for model in models]
            info["model_loaded"] = OLLAMA_MODEL in info["available_models"]

    except Exception as e:
        info["available"] = False
        info["error"] = str(e)

    return info


def test_ai_service():
    """AI 서비스 연결 및 기능 테스트"""
    try:
        info = get_ai_provider_info()
        print(f"[AI] 제공자 정보: {info}")

        # 간단한 테스트 텍스트로 요약 생성
        test_text = """안녕하세요. 오늘 회의에서는 프로젝트 진행 상황을 점검하고 다음 주 마감일에 대해 논의했습니다. 
        김 대리는 UI 디자인을 완료하기로 했고, 박 과장은 데이터베이스 설계를 검토하기로 했습니다. 
        최종 배포는 다음 달 15일로 결정되었습니다."""

        if info["available"]:
            summary, action_items, decisions = generate_summary(test_text)
            print(f"[AI] 테스트 요약: {summary[:50]}...")
            return True
        else:
            print(f"[AI] Ollama를 사용할 수 없습니다")
            return False

    except Exception as e:
        print(f"[AI] 테스트 실패: {e}")
        return False


if __name__ == "__main__":
    test_ai_service()
