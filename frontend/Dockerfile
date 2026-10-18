# syntax=docker/dockerfile:1

FROM node:20-alpine AS build
WORKDIR /app/frontend

RUN corepack enable

COPY frontend/package.json frontend/pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile

COPY frontend .
RUN pnpm build

FROM nginx:1.27-alpine

COPY --from=build /app/frontend/dist /usr/share/nginx/html
COPY deploy/nginx/prod.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

