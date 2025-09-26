FROM node:20-bookworm

# 프리빌트 워커 사용 금지 + io_uring 비활성화 (code:2 회피)
ENV MEDIASOUP_SKIP_WORKER_PREBUILT_DOWNLOAD=true
ENV MESON_ARGS="-Dms_disable_liburing=true"

WORKDIR /usr/src/app

# 빌드/런타임 의존성 + pip 추가
RUN apt-get update && apt-get install -y --no-install-recommends \
    python3 python3-pip make g++ pkg-config ninja-build meson \
    libssl-dev libsrtp2-dev \
    libstdc++6 libgcc-s1 libatomic1 libuv1 libsrtp2-1 libssl3 ca-certificates \
 && rm -rf /var/lib/apt/lists/*

COPY package*.json ./
RUN npm ci --omit=dev --no-audit --no-fund

COPY . .

CMD ["node","src/server.js"]
