# --- Build stage ---
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
ARG MAVEN_ARGS="-B"
# Par défaut on exécute les tests; pour les skipper : --build-arg MAVEN_ARGS="-B -DskipTests"
RUN mvn ${MAVEN_ARGS} package spring-boot:repackage

# --- Run stage ---
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Render définit automatiquement la variable d'environnement $PORT
ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-Dserver.port=${PORT}", "-jar", "/app/app.jar"]
