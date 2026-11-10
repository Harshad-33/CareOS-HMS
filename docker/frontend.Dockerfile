# Development Dockerfile for CareOS-HMS Frontend (Angular + Angular Material)
FROM node:24-alpine

WORKDIR /app

COPY frontend/package.json frontend/package-lock.json* ./
RUN npm install --no-audit --no-fund

COPY frontend/ .

EXPOSE 4200

HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=5 \
  CMD node -e "fetch('http://127.0.0.1:4200').then(r => process.exit(r.ok ? 0 : 1)).catch(() => process.exit(1))"

CMD ["npx", "ng", "serve", "--host", "0.0.0.0", "--port", "4200"]
