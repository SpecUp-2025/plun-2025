# app/services/summary_llm.py
import re
import requests

# --- 고정값(간단하게 하드코딩) ---
OLLAMA_HOST = "http://127.0.0.1:11434"
MODEL = "qwen2.5:3b-instruct-q4_K_M"  # 품질 우선. 느리면 phi3:mini로 교체
CONNECT_TIMEOUT = 3.0
READ_TIMEOUT = 120.0                  # 첫 로딩 대비 여유
INPUT_LIMIT = 1500

# 완료 마커: 모델이 끝에 반드시 붙이도록 지시 → 수신 후 제거
ENDMARK = "<!-- END -->"

# 생성 길이(부족시 1회 재시도 길이 상향)
NUM_PREDICT = 480
NUM_PREDICT_RETRY = 640

# 코드펜스 제거용 정규식
_FENCE_START = re.compile(r"^\s*```[\w-]*\s*\n?", re.MULTILINE)
_FENCE_END   = re.compile(r"\n?\s*```\s*$", re.MULTILINE)

_PROMPT = f"""다음 전사 텍스트를 바탕으로 간결한 한국어 회의록을 Markdown으로 작성하세요.

# 회의 요약
- 핵심 사항을 3~6개 불릿으로

## 액션 아이템
- [ ] 실행 항목 (담당: , 기한: )

## 결정 사항
- 합의/확정된 내용 2~5개

규칙:
- 텍스트 외 설명 없이 **마크다운 본문만** 출력
- **코드펜스( ``` ) 및 언어 태그 절대 사용 금지**
- 모든 섹션(회의 요약/액션 아이템/결정 사항)을 반드시 포함
- 마지막 줄에 반드시 {ENDMARK} 를 출력

전사:
\"\"\"{{body}}\"\"\""""

def _call_ollama(prompt: str, num_predict: int) -> str:
    url = f"{OLLAMA_HOST.rstrip('/')}/api/generate"
    payload = {
        "model": MODEL,
        "prompt": prompt,
        "stream": False,
        "options": {"num_predict": num_predict, "temperature": 0.2},
    }
    r = requests.post(
        url,
        json=payload,
        headers={"Connection": "close"},           # Win/프록시 이슈 최소화
        timeout=(CONNECT_TIMEOUT, READ_TIMEOUT),   # (connect, read)
    )
    r.raise_for_status()
    return (r.json().get("response") or "").strip()

def _sanitize_markdown(md: str) -> str:
    """코드펜스 제거 + 필수 섹션 보정."""
    s = (md or "").strip()
    if s.startswith("```"):
        s = _FENCE_START.sub("", s, count=1)
        s = _FENCE_END.sub("", s, count=1)
        s = s.strip()
    if not s.startswith("# 회의 요약"):
        s = "# 회의 요약\n" + s
    if "## 액션 아이템" not in s:
        s += "\n\n## 액션 아이템\n- (없음)"
    if "## 결정 사항" not in s:
        s += "\n\n## 결정 사항\n- (없음)"
    return s

def generate_minutes_md(text: str) -> str:
    body = (text or "")[:INPUT_LIMIT]
    prompt = _PROMPT.format(body=body)

    # 1차 생성
    md = _call_ollama(prompt, NUM_PREDICT)
    if ENDMARK not in md:
        # 2차(1회만) — 더 길게
        md = _call_ollama(prompt, NUM_PREDICT_RETRY)

    if not md:
        raise RuntimeError("empty LLM response")

    # 마커 제거
    if ENDMARK in md:
        md = md.replace(ENDMARK, "").rstrip()

    # 최종 정리
    md = _sanitize_markdown(md)
    return md
