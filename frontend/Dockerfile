# Multi-stage Dockerfile for frontend (Astro + Vite)
# Build with Node 18, serve static files with nginx on port 5174

############################
# Builder stage
############################
FROM node:18-alpine AS builder

WORKDIR /app

# Copy package manifests only to leverage Docker cache
COPY package.json package-lock.json* ./

# Install dependencies: prefer npm ci when lockfile present, otherwise fallback to npm install
RUN if [ -f package-lock.json ]; then npm ci --silent; else npm install --silent; fi

# Copy source and build
COPY . .
RUN npm run build && \
    # normalize common output dirs to /app/dist so final stage can copy consistently
    if [ -d ./dist ]; then echo "dist exists"; \
    elif [ -d ./build ]; then mv ./build ./dist || true; \
    elif [ -d ./out ]; then mv ./out ./dist || true; \
    else echo "No build output found"; fi && \
    echo "--- build output listing ---" && ls -la ./dist || true

############################
# Production image
############################
FROM nginx:stable-alpine

# Replace default nginx config with our custom one (listens on 5174)
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy built static files from builder
COPY --from=builder /app/dist /usr/share/nginx/html

# Ensure files are readable by nginx user
RUN chown -R nginx:nginx /usr/share/nginx/html

# Ensure entrypoint and any init scripts are executable/readable to avoid permission issues
RUN if [ -f /docker-entrypoint.sh ]; then chmod 755 /docker-entrypoint.sh; fi && \
    if [ -d /docker-entrypoint.d ]; then chmod -R 755 /docker-entrypoint.d || true; fi

# Expose the port the app will run on
EXPOSE 5174

# NOTE: Do NOT switch to USER nginx here — keep default user so the official
# docker-entrypoint.sh can run initialization scripts as root. The entrypoint
# will drop privileges as needed. Setting USER nginx earlier caused
# "/docker-entrypoint.sh: exec: line 47: .: Permission denied".

# Instead of relying on the official image ENTRYPOINT (/docker-entrypoint.sh)
# (which in some environments may fail to source scripts), override the
# ENTRYPOINT to directly run nginx. This avoids the permission-denied when
# sourcing files inside /docker-entrypoint.d while still serving static files.
ENTRYPOINT ["nginx", "-g", "daemon off;"]

# Start nginx in foreground (kept for compatibility; ENTRYPOINT already handles it)
#CMD ["/bin/sh","-c","nginx -g 'daemon off;'" ]
