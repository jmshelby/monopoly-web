# Build stage
FROM clojure:temurin-17-alpine AS builder

# Install Node.js
RUN apk add --no-cache nodejs npm

WORKDIR /app

# Copy project files
COPY deps.edn shadow-cljs.edn package.json ./
COPY src/ ./src/
COPY resources/ ./resources/

# Install dependencies
RUN npm install

# Build the application
RUN npm run build

# Production stage
FROM nginx:alpine

# Copy built assets
COPY --from=builder /app/resources/public /usr/share/nginx/html

# Copy nginx config
COPY docker/nginx.conf /etc/nginx/conf.d/default.conf

# Expose port
EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]