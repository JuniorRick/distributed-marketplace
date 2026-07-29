FROM node:24-alpine3.24 AS build

ARG SERVICE
WORKDIR /workspace

COPY package.json package-lock.json ./
COPY apps/catalog/frontend/package.json ./apps/catalog/frontend/package.json
COPY apps/cart/frontend/package.json ./apps/cart/frontend/package.json
COPY apps/orders/frontend/package.json ./apps/orders/frontend/package.json
COPY packages/marketplace-ui/package.json ./packages/marketplace-ui/package.json
RUN npm ci

COPY packages/marketplace-ui ./packages/marketplace-ui
COPY apps/${SERVICE}/frontend ./apps/${SERVICE}/frontend
RUN npm --prefix apps/${SERVICE}/frontend run build

FROM nginx:1.31.3-alpine

ARG SERVICE
COPY docker/nginx/${SERVICE}.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/apps/${SERVICE}/frontend/dist /usr/share/nginx/html
