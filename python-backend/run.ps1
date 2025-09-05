if (Test-Path ".\whisper-env\Scripts\Activate.ps1") {
    . .\whisper-env\Scripts\Activate.ps1
} else {
    Write-Host "가상환경 없음. python -m venv whisper-env 로 먼저 생성하세요."
    exit 1
}

python -m uvicorn app.main:app --reload --port 8000
