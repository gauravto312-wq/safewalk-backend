# ============================================================
# Stage 1: Build Stage (Maven + JDK 17)
# ============================================================
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder
WORKDIR /app

# Copy dependency manifest to leverage Docker layer caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy application source code and compile output JAR
COPY src ./src
RUN mvn package -DskipTests -B

# ============================================================
# Stage 2: Runtime Stage (Lightweight JRE 17)
# ============================================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create non-root system user and group for security
RUN addgroup -S safewalk && adduser -S safewalk -G safewalk

# Ensure application and uploads directories are writable by safewalk user
RUN mkdir -p /app/uploads && chown -R safewalk:safewalk /app

# Copy compiled JAR artifact from build stage with correct permissions
COPY --chown=safewalk:safewalk --from=builder /app/target/safewalk-backend-1.0.0-SNAPSHOT.jar app.jar

USER safewalk

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
