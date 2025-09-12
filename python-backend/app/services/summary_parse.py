# app/services/summary_parse.py
import re

def extract_section(md: str, header_kr: str) -> str:
    """
    마크다운에서 '## <header_kr>' 섹션의 본문만 추출.
    다음 '## ' 이전까지를 범위로 잡고, 불릿 라인을 정규화해서 반환.
    """
    if not md:
        return ""
    pattern = rf"(?ms)^\s*##\s*{re.escape(header_kr)}\s*\n(.*?)(?=^\s*##\s|\Z)"
    m = re.search(pattern, md)
    if not m:
        return ""
    body = m.group(1).strip()
    if not body:
        return ""

    lines = []
    for raw in body.splitlines():
        t = raw.strip()
        if not t:
            continue
        if t.startswith(("- [", "- ", "* ")):
            lines.append(t)
        else:
            lines.append(f"- {t}")
    return "\n".join(lines)
