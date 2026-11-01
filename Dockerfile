FROM node:24-alpine AS frontend
WORKDIR /app
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM eclipse-temurin:25-jdk-noble AS backend
WORKDIR /app
COPY gradlew build.gradle settings.gradle ./
COPY gradle/ ./gradle/
COPY src/ ./src/
COPY --from=frontend /app/dist ./frontend/dist/
RUN --mount=type=cache,target=/root/.gradle ./gradlew --no-daemon test bootJar

FROM eclipse-temurin:25-jre-noble
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=backend /app/build/libs/*.jar app.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
