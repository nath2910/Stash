FROM node:20-bookworm-slim AS tracking-deps
WORKDIR /app/tracking-scripts
COPY backend/tracking-scripts/package*.json ./
RUN npm ci --omit=dev --no-audit --no-fund

FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY backend/pom.xml ./pom.xml
COPY backend/src ./src
ARG MAVEN_ARGS="-B"
RUN mvn ${MAVEN_ARGS} package spring-boot:repackage

FROM eclipse-temurin:17-jre
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates nodejs chromium \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/*.jar /app/app.jar
COPY backend/tracking-scripts /app/tracking-scripts
COPY --from=tracking-deps /app/tracking-scripts/node_modules /app/tracking-scripts/node_modules
COPY backend/docker-entrypoint.sh /app/docker-entrypoint.sh

RUN chmod +x /app/docker-entrypoint.sh

ENV PORT=8080
ENV PUPPETEER_EXECUTABLE_PATH=/usr/bin/chromium
EXPOSE 8080

ENTRYPOINT ["/app/docker-entrypoint.sh"]
