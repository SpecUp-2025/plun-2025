from fastapi import FastAPI

app = FastAPI(title="STT Service (step1)")

@app.get("/healthz")
def healthz():
    return {"status": "ok"}