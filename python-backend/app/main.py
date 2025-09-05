from fastapi import FastAPI
from fastapi.staticfiles import StaticFiles
from app.routers.stt import router as stt_router

app = FastAPI(title="STT (modular minimal)")
app.include_router(stt_router)
app.mount("/static", StaticFiles(directory="static"), name="static")
