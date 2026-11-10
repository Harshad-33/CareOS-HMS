# Development Dockerfile for CareOS-HMS Backend (Java 21 + Spring Boot 3.x)
FROM maven:3.9.12-eclipse-temurin-21

WORKDIR /app

# Pre-cache Maven dependencies when building standalone image
COPY backend/pom.xml ./pom.xml
RUN mvn dependency:resolve -B

# Copy backend source and shared database migrations
COPY backend/src ./src
COPY database/migrations /database/migrations
RUN mvn compile -B

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
  CMD curl -fsS http://localhost:8080/api/v1/health || exit 1

CMD ["mvn", "spring-boot:run"]
