# syntax=docker/dockerfile:1
# Imagem do frontend — build multi-stage. Imagens com versão fixada (nunca "latest").

# ---- build ----
FROM node:24.22.3-alpine AS build
WORKDIR /build
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

# ---- runtime ----
# nginx sem root (escuta na porta 8080).
FROM nginxinc/nginx-unprivileged:1.31.7-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /build/dist/clinica-viene/browser /usr/share/nginx/html
EXPOSE 8080
