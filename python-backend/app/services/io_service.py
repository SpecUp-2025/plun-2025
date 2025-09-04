import json
from pathlib import Path
from typing import Dict, Tuple, List
from fastapi import HTTPException

def append_index(index_path: Path, meta: dict) -> None:
    index_path.parent.mkdir(parents=True, exist_ok=True)
    with open(index_path, "a", encoding="utf-8") as f:
        f.write(json.dumps(meta, ensure_ascii=False) + "\n")

def read_index_unique(index_path: Path) -> List[dict]:
    if not index_path.exists():
        raise HTTPException(404, "index not found")
    seen: Dict[Tuple[int,int], dict] = {}
    with open(index_path, "r", encoding="utf-8") as f:
        for line in f:
            try:
                r = json.loads(line)
                key = (int(r.get("user_no", 0)), int(r.get("seq", 0)))
                if key not in seen: seen[key] = r
                else:
                    if r.get("ts_ms", 0) < seen[key].get("ts_ms", 0):
                        seen[key] = r
            except:
                pass
    recs = list(seen.values())
    recs.sort(key=lambda r: (r.get("ts_ms", 0), r.get("user_no", 0), r.get("seq", 0)))
    return recs
