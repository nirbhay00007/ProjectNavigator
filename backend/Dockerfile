# ─── Node.js ML Backend — Multi-stage Dockerfile ──────────────────────────────
# Stage 1: Build (compile TypeScript)
FROM node:20-alpine AS builder
WORKDIR /app
# Install git so the pipeline can clone GitHub repositories
RUN apk add --no-cache git
COPY package*.json ./
RUN npm install --legacy-peer-deps
COPY . .
# tsx runs TypeScript directly so no explicit tsc compile step is needed

# Stage 2: Runtime image (lean production layer)
FROM node:20-alpine AS runner
WORKDIR /app
ENV NODE_ENV=production

# Install git so the pipeline can clone GitHub repositories at runtime
RUN apk add --no-cache git

COPY package*.json ./
RUN npm install --omit=dev --legacy-peer-deps

# Copy source (tsx evaluates at runtime, no build artefacts needed)
COPY --from=builder /app/src ./src
COPY --from=builder /app/tsconfig.json ./

# Port the Express server listens on
EXPOSE 3001

# tsx runs TypeScript source directly in production
CMD ["npx", "tsx", "src/index.ts"]
