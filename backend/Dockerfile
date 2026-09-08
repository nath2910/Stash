# --- Tracking scripts deps stage ---
FROM node:20-bookworm-slim AS tracking-deps
WORKDIR /app/tracking-scripts
COPY tracking-scripts/package*.json ./
RUN npm ci --omit=dev --no-audit --no-fund

# --- Build stage ---
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
ARG MAVEN_ARGS="-B"
# Par defaut on execute les tests; pour les skipper : --build-arg MAVEN_ARGS="-B -DskipTests"
RUN mvn ${MAVEN_ARGS} package spring-boot:repackage

# --- Run stage ---
FROM eclipse-temurin:17-jre
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates nodejs chromium \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/*.jar /app/app.jar
COPY tracking-scripts /app/tracking-scripts
COPY --from=tracking-deps /app/tracking-scripts/node_modules /app/tracking-scripts/node_modules
COPY docker-entrypoint.sh /app/docker-entrypoint.sh

RUN chmod +x /app/docker-entrypoint.sh

ENV PORT=8080
ENV SPRING_PROFILES_ACTIVE=prod
ENV PUPPETEER_EXECUTABLE_PATH=/usr/bin/chromium
EXPOSE 8080
RUN useradd --system --uid 10001 --create-home stash && mkdir -p /app/uploads && chown -R stash:stash /app
USER stash

ENTRYPOINT ["/app/docker-entrypoint.sh"]
