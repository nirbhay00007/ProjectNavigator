# ─── Vite React Frontend — Multi-stage Dockerfile ─────────────────────────────
#
# Stage 1: Build — installs deps and runs `vite build` to produce /dist
# Stage 2: Runtime — serves the static dist/ folder via nginx
#
# Build command:
#   docker build -t devclash/frontend:latest .
#
# Run command:
#   docker run -p 5173:80 devclash/frontend:latest

# ─── Stage 1: Build ───────────────────────────────────────────────────────────
FROM node:20-alpine AS builder

WORKDIR /app

# Copy package files first for layer caching
COPY package*.json ./
RUN npm install

# Copy all source files and build
COPY . .

# VITE_API_URL can be overridden at build time:
#   docker build --build-arg VITE_API_URL=https://your-backend.com -t devclash/frontend .
ARG VITE_API_URL=http://localhost:3001
ENV VITE_API_URL=$VITE_API_URL

RUN npm run build

# ─── Stage 2: Runtime (nginx) ──────────────────────────────────────────────────
FROM nginx:alpine AS runner

# Remove default nginx static content
RUN rm -rf /usr/share/nginx/html/*

# Copy the Vite build output into nginx's serve directory
COPY --from=builder /app/dist /usr/share/nginx/html

# Copy custom nginx config to handle React Router (SPA fallback to index.html)
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Nginx listens on port 80 inside the container
# The docker-compose.yml maps this to host port 5173
EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
