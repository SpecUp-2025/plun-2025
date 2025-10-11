# 🎯 Plun - 통합 협업 플랫폼

> 화상회의, 실시간 채팅, AI 기반 회의록 자동 생성을 하나의 플랫폼에서 제공

<br>

[![Figma](https://img.shields.io/badge/Figma-디자인_보기-F24E1E?logo=figma&logoColor=white)](https://www.figma.com/slides/k2Lt0jTwz4oYKNvNJ5zgDp/2025-KOSA-SPECUP)

<br>

## 📋 목차
- [프로젝트 소개](#-프로젝트-소개)
- [주요 기능](#-주요-기능)
- [기술 스택](#-기술-스택)
- [시스템 아키텍처](#-시스템-아키텍처)
- [팀 구성](#-팀-구성)
- [설치 및 실행](#-설치-및-실행)

<br>

## 🎯 프로젝트 소개

### 개요
- **프로젝트명**: Plun (Plan + Run)
- **개발 기간**: 2025.08 ~ 2025.10 (3개월)
- **팀 구성**: 3명
- **한 줄 소개**: 채팅, 일정, 화상회의, AI 회의록을 하나로 통합한 협업 솔루션

### 핵심 가치
- **통합 협업 환경**: 여러 툴을 오가지 않고 하나의 플랫폼에서 모든 협업 진행
- **AI 자동화**: 회의록 작성 시간 90% 단축 (음성 전사 + 자동 요약)
- **실시간 동기화**: WebSocket 기반 실시간 채팅, 알림, 일정 공유

<br>

## ✨ 주요 기능

### 1️⃣ 화상회의 (WebRTC + mediasoup SFU)
- 다자간 실시간 화상회의 (1:1 → 1:N 확장)
- 화면 공유, 음소거, 카메라 제어
- 네트워크 불안정 시 재연결 처리

### 2️⃣ AI 회의록 자동화
```
음성 녹음 → 전처리(FFmpeg) → STT(Whisper) → AI 요약(Ollama) → 회의록 생성
```
- 실시간 음성 → 텍스트 변환
- 회의 종료 즉시 요약본 자동 생성
- 핵심 내용, 결정사항, 액션 아이템 자동 추출

### 3️⃣ 실시간 채팅 & 알림
- WebSocket 기반 실시간 양방향 통신
- 1:1 채팅 및 팀 채팅
- 파일/이미지 공유
- 실시간 알림 배지 (채팅, 일정, 멘션)

### 4️⃣ 캘린더 & 일정 관리
- FullCalendar 연동 (월/주/일 뷰)
- 회의 일정 자동 연동
- 일정 등록 시 팀원에게 실시간 반영
- 회의 시작 10분 전 자동 알림

### 5️⃣ 회원 & 인증
- JWT 기반 Access/Refresh Token
- OAuth 2.0 소셜 로그인 (Google, Kakao, Naver)
- 이메일 인증
- 팀 생성 및 관리

<br>

## 🛠 기술 스택

### Frontend
![Vue.js](https://img.shields.io/badge/Vue.js-3.5-4FC08D?logo=vue.js&logoColor=white)
![Vite](https://img.shields.io/badge/Vite-7.0-646CFF?logo=vite&logoColor=white)
![Pinia](https://img.shields.io/badge/Pinia-3.0-yellow?logo=pinia&logoColor=white)

- Vue.js 3.5 | Vite | Pinia | Vue Router
- Axios | Socket.io-client | @stomp/stompjs
- mediasoup-client | FullCalendar

### Backend
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5-6DB33F?logo=spring-boot&logoColor=white)
![Python](https://img.shields.io/badge/Python-3.10-3776AB?logo=python&logoColor=white)
![Node.js](https://img.shields.io/badge/Node.js-20-339933?logo=node.js&logoColor=white)

**Spring Boot** (Java 17)
- Spring Security | JWT | MyBatis
- WebSocket | STOMP | Spring Mail
- Redis | MariaDB

**Python FastAPI**
- Faster Whisper (STT)
- Ollama (로컬 LLM)
- FFmpeg (음성 전처리)

**Node.js**
- mediasoup (SFU)
- Socket.io (시그널링)
- Express

### Database & Cache
![MariaDB](https://img.shields.io/badge/MariaDB-003545?logo=mariadb&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?logo=redis&logoColor=white)

### Infrastructure
![Docker](https://img.shields.io/badge/Docker-2496ED?logo=docker&logoColor=white)
![Azure](https://img.shields.io/badge/Azure-0078D4?logo=microsoft-azure&logoColor=white)
![GitHub Actions](https://img.shields.io/badge/GitHub_Actions-2088FF?logo=github-actions&logoColor=white)

<br>

## 🏗 시스템 아키텍처

### 멀티 언어 백엔드 구조

```
                      ┌─────────────┐
                      │   사용자    │
                      └──────┬──────┘
                             │
           ┌─────────────────┼─────────────────┐
           │                 │                 │
           ▼                 ▼                 ▼
      ┌────────┐        ┌────────┐       ┌──────────┐
      │Frontend│        │ Spring │       │  Node.js │
      │ Vue.js │◄──────►│  Boot  │◄─────►│   SFU    │
      └────────┘        └───┬────┘       └──────────┘
                            │
                ┌───────────┼───────────┐
                │           │           │
                ▼           ▼           ▼
           ┌────────┐  ┌────────┐  ┌────────┐
           │ Python │  │MariaDB │  │ Redis  │
           │FastAPI │  └────────┘  └────────┘
           └───┬────┘
               │
               ▼
          ┌────────┐
          │ Ollama │
          └────────┘
```

### 서비스별 역할

| 서비스 | 역할 | 주요 기능 |
|--------|------|-----------|
| **Spring Boot** | 메인 백엔드 | 인증, 회원, 팀, 회의실, 채팅, 알림, 캘린더 |
| **Python FastAPI** | AI 처리 | 음성 전사(STT), 회의록 요약 |
| **Node.js** | 화상회의 | WebRTC 시그널링, mediasoup SFU |
| **Redis** | 캐싱 | JWT 토큰, 알림 큐, 세션 관리 |
| **Ollama** | AI 모델 | 로컬 LLM (llama3.2:3b) |

<br>

## 👥 팀 구성

### 역할 분담

| 이름 | 담당 영역 | 주요 기능 |
|------|----------|-----------|
| **김수영** | 회원 관리 & 배포 | 로그인, 회원가입, OAuth 2.0, 이메일 인증, 팀 관리, Azure 배포 |
| **김석률** | 화상회의 & AI 요약 | WebRTC, mediasoup SFU, Whisper STT, Ollama 요약, 멀티 백엔드 통합 |
| **조용민** | 실시간 통신 & 일정 | 실시간 채팅, 실시간 알림, FullCalendar, 파일 공유 |

<br>

## 📦 설치 및 실행

### 사전 요구사항
- Docker & Docker Compose

### Quick Start
```bash
# 1. 저장소 클론
git clone https://github.com/SpecUp-2025/plun-2025.git
cd plun-2025

# 2. 환경 변수 설정
cp .env template .env
# .env 파일 수정 (DB 비밀번호, JWT Secret 등)

# 3. Ollama 볼륨 생성
docker volume create plun_ollama_data

# 4. 서비스 실행
docker-compose up -d

# 5. Ollama 모델 다운로드
docker exec -it plun-ollama ollama pull llama3.2:3b
```

### 접속
- **Frontend**: http://localhost
- **Backend API**: http://localhost:8080
- **SFU Server**: http://localhost:4000
- **Python Backend**: http://localhost:8000

<br>

## 📁 프로젝트 구조

```
plun-2025/
├── backend/              # Spring Boot (Java 17)
│   └── src/main/java/com/spec/plun/
│       ├── auth/        # JWT, OAuth 2.0
│       ├── member/      # 회원 관리
│       ├── team/        # 팀 관리
│       ├── meeting/     # 회의실 관리
│       ├── chat/        # 실시간 채팅
│       ├── alarm/       # 실시간 알림
│       └── calendar/    # 캘린더
│
├── python-backend/       # FastAPI (AI/ML)
│   └── app/
│       ├── routes/      # API
│       └── services/    # STT, 요약
│
├── sfu-server/          # Node.js (WebRTC)
│   └── src/
│       └── server.js    # mediasoup SFU
│
├── frontend/            # Vue.js 3
│   └── src/
│       ├── components/
│       ├── router/
│       └── stores/
│
└── docker-compose.yml
```

<br>

## 🔥 핵심 기술 포인트

### 1. 멀티 언어 백엔드 아키텍처
- Spring Boot, Python FastAPI, Node.js를 각 기능에 최적화
- REST API 및 HTTP 프록시로 서비스 간 통신
- 각 언어의 강점을 살린 효율적인 설계

### 2. WebRTC + mediasoup SFU
- P2P → SFU 구조로 확장하며 다자간 통신 구현
- Socket.io 기반 시그널링 서버
- 에코 방지 및 재연결 로직

### 3. AI 음성 전사 및 요약 파이프라인
- Whisper 모델로 음성 → 텍스트 변환
- Ollama 로컬 LLM으로 회의록 자동 요약
- 외부 API 비용 없이 데이터 프라이버시 확보
- **회의록 작성 시간 90% 단축**

### 4. 실시간 통신 (WebSocket)
- STOMP 프로토콜 기반 실시간 채팅
- Redis 기반 빠른 상태 관리
- 알림 큐로 메시지 유실 방지

<br>

## 🤝 협업 방식

- **Git Flow**: main 브랜치 아래 기능별 브랜치
- **일일 스크럼**: 매일 오전 9시 Discord 30분 회의
- **API 문서화**: 백엔드-프론트엔드 API 명세 사전 정의

<br>

## 📞 문의

- **GitHub Organization**: [SpecUp-2025](https://github.com/SpecUp-2025)
- **Project Repository**: [plun-2025](https://github.com/SpecUp-2025/plun-2025)

<br>

## 📄 License

This project is licensed under the MIT License.

---

<div align="center">

**Made with ❤️ by KOSA 1ST SPEC-UP Team**

</div>
