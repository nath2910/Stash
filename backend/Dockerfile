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
    && apt-get install -y --no-install-recommends nodejs npm chromium \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/*.jar app.jar
COPY tracking-scripts ./tracking-scripts

RUN cd /app/tracking-scripts && npm install --omit=dev

ENV PORT=8080
ENV PUPPETEER_EXECUTABLE_PATH=/usr/bin/chromium
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT} -jar /app/app.jar"]
