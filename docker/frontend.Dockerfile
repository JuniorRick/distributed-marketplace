# syntax=docker/dockerfile:1.7
FROM node:24-alpine3.24 AS build

ARG SERVICE
ARG VITE_OIDC_URL=http://localhost:8090
ARG VITE_OIDC_REALM=marketplace
ARG VITE_OIDC_CLIENT_ID=marketplace-spa
ARG VITE_MARKETPLACE_URL=http://localhost:8080
ARG VITE_AUTH_ENABLED=true
ENV VITE_OIDC_URL=${VITE_OIDC_URL}
ENV VITE_OIDC_REALM=${VITE_OIDC_REALM}
ENV VITE_OIDC_CLIENT_ID=${VITE_OIDC_CLIENT_ID}
ENV VITE_MARKETPLACE_URL=${VITE_MARKETPLACE_URL}
ENV VITE_AUTH_ENABLED=${VITE_AUTH_ENABLED}
WORKDIR /workspace

COPY package.json package-lock.json ./
COPY apps/catalog/frontend/package.json ./apps/catalog/frontend/package.json
COPY apps/cart/frontend/package.json ./apps/cart/frontend/package.json
COPY apps/orders/frontend/package.json ./apps/orders/frontend/package.json
COPY packages/marketplace-ui/package.json ./packages/marketplace-ui/package.json
COPY packages/marketplace-auth/package.json ./packages/marketplace-auth/package.json
RUN --mount=type=cache,id=marketplace-npm,target=/root/.npm,sharing=locked npm ci

COPY packages/marketplace-ui ./packages/marketplace-ui
COPY packages/marketplace-auth ./packages/marketplace-auth
COPY apps/${SERVICE}/frontend ./apps/${SERVICE}/frontend
RUN npm --prefix apps/${SERVICE}/frontend run build

FROM nginx:1.31.3-alpine

ARG SERVICE
COPY docker/nginx/${SERVICE}.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/apps/${SERVICE}/frontend/dist /usr/share/nginx/html
