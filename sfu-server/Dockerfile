FROM node:20-bullseye

WORKDIR /usr/src/app

RUN apt-get update && \
    apt-get install -y python3 python3-pip make g++ pkg-config \
    && ln -sf /usr/bin/python3 /usr/bin/python 

COPY package*.json ./

RUN npm install

COPY . .

CMD ["node", "server.js"]