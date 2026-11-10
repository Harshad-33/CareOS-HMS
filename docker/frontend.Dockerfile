# Development Dockerfile for CareOS-HMS Frontend (Angular + Angular Material)
FROM node:24-alpine

WORKDIR /app

COPY frontend/package.json frontend/package-lock.json* ./
RUN npm install --no-audit --no-fund

COPY frontend/ .

EXPOSE 4200

CMD ["npx", "ng", "serve", "--host", "0.0.0.0", "--port", "4200"]
