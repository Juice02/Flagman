# --- Build stage ---
FROM node:22.21.1-alpine AS build
WORKDIR /src

# Install dependencies first so they are cached between code changes.
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# --- Runtime stage: static files plus a proxy from /api to the API container ---
FROM nginx:1.29.3-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /src/dist /usr/share/nginx/html
EXPOSE 80
