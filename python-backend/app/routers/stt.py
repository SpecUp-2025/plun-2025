from fastapi import APIRouter, UploadFile, File, HTTPException, Form, Query
from fastapi.responses import PlainTextResponse, JSONResponse
from pathlib import Path
import shutil, time
from typing import List
from app.config import CHUNK_ROOT, MODEL_NAME
from app.services.audio_service import preconvert_webm_to_wav, merge_wavs_in_order
from app.services.io_service import append_index, read_index_unique
from app.services.stt_service import transcribe_ko

router = APIRouter()

@router.get("/healthz")
def healthz():
    return {"status": "ok", "model": MODEL_NAME}

@router.post("/stt/chunk_raw")
async def stt_chunk_raw(
    file: UploadFile = File(...),
    room_no: int = Form(...),
    user_no: int = Form(...),
    seq: int   = Form(...),
    ts_ms: int = Form(...),
):
    room_dir = CHUNK_ROOT / str(room_no)
    room_user_dir = room_dir / str(user_no)
    room_user_dir.mkdir(parents=True, exist_ok=True)
    src = room_user_dir / f"{seq:04d}.webm"
    src.write_bytes(await file.read())
    index_path = room_dir / "index.jsonl"
    append_index(index_path, {"user_no": user_no, "seq": seq, "ts_ms": ts_ms, "path": str(src)})
    pre_wav = room_user_dir / f"{seq:04d}.wav"
    pre_ok = preconvert_webm_to_wav(src, pre_wav)
    return {"ok": True, "saved": str(src), "bytes": src.stat().st_size,
            "wav_preconverted": pre_ok, "wav_path": str(pre_wav) if pre_ok else None}

@router.get("/stt/chunk_index")
def stt_chunk_index(room_no: int = Query(...)):
    room_dir = CHUNK_ROOT / str(room_no)
    index_path = room_dir / "index.jsonl"
    rows = read_index_unique(index_path)
    return {"room_no": room_no, "count": len(rows), "records": rows}

@router.post("/stt/merge_room", response_class=PlainTextResponse)
def stt_merge_room(room_no: int):
    room_dir = CHUNK_ROOT / str(room_no)
    index_path = room_dir / "index.jsonl"
    recs = read_index_unique(index_path)
    wav_parts: List[Path] = []
    for i, r in enumerate(recs):
        src_webm = Path(r["path"])
        if not src_webm.exists(): continue
        pre_wav = src_webm.with_suffix(".wav")
        if not pre_wav.exists() or pre_wav.stat().st_size == 0:
            continue
        wav_parts.append(pre_wav)
    if not wav_parts: raise HTTPException(400, "no wav parts available")
    merged = room_dir / f"room_{room_no}.wav"
    elapsed = merge_wavs_in_order(wav_parts, merged)
    return ("✅ 병합 완료\n"
            f"- room_no: {room_no}\n"
            f"- 유니크 청크 수: {len(recs)}\n"
            f"- 사용된 wav 파트: {len(wav_parts)}\n"
            f"- 결과 파일: {merged}\n"
            f"- ⏱ 병합 소요: {elapsed:.2f}s\n")

@router.post("/stt/finalize_room", response_class=PlainTextResponse)
def stt_finalize_room(room_no: int):
    t_all = time.time()
    room_dir = CHUNK_ROOT / str(room_no)
    merged = room_dir / f"room_{room_no}.wav"
    t_merge = 0.0
    if not merged.exists() or merged.stat().st_size == 0:
        _ = stt_merge_room(room_no)
        if not merged.exists() or merged.stat().st_size == 0:
            raise HTTPException(400, "merge failed or no merged file")
    text, t_stt = transcribe_ko(merged)
    out_txt = room_dir / f"room_{room_no}.txt"
    out_txt.write_text(text, encoding="utf-8")
    t_total = time.time() - t_all
    head = text[:400].replace("\n", " ")
    return ("✅ 전사 완료 (openai-whisper, single-pass)\n"
            f"- model: {MODEL_NAME}\n"
            f"- room_no: {room_no}\n"
            f"- 병합 파일: {merged}\n"
            f"- 텍스트 저장: {out_txt}\n"
            "\n"
            f"⏱ 소요 시간\n"
            f"- 전사: {t_stt:.2f}s\n"
            f"- 총합: {t_total:.2f}s\n"
            "\n미리보기(400자):\n" + head + "\n")

@router.delete("/stt/reset_room", response_class=PlainTextResponse)
def stt_reset_room(room_no: int):
    room_dir = CHUNK_ROOT / str(room_no)
    if room_dir.exists():
        shutil.rmtree(room_dir)
    return f"🧹 방 {room_no} 데이터 삭제 완료"
