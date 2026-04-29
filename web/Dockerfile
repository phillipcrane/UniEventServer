# Build stage
FROM node:20-alpine AS builder
WORKDIR /app

# Install dependencies (cached unless package*.json changes)
COPY package*.json ./
RUN npm ci

# Declare VITE_* build-time vars - baked into the JS bundle at build time
ARG VITE_BACKEND_URL
ARG VITE_FACEBOOK_APP_ID

ENV VITE_BACKEND_URL=$VITE_BACKEND_URL \
    VITE_FACEBOOK_APP_ID=$VITE_FACEBOOK_APP_ID

COPY . .
RUN npm run build

# Runtime stage - nginx serves static files
FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 3000
CMD ["nginx", "-g", "daemon off;"]
