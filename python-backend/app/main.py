from fastapi import FastAPI
from fastapi.staticfiles import StaticFiles
from app.routers.stt import router as stt_router
from app.routers.transcript import router as transcript_router
from app.routers.summary import router as summary_router


app = FastAPI(title="STT (modular minimal)")
app.include_router(stt_router)
app.include_router(transcript_router)
app.mount("/static", StaticFiles(directory="static"), name="static")
app.include_router(summary_router)
