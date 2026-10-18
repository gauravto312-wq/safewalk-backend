# SafeWalk V1 — Real-time Emergency & Assistance System

SafeWalk is a real-time emergency and community assistance platform built with Spring Boot, MySQL, Redis GEO, Kafka, WebSockets, Firebase FCM, and Google Maps API.

---

## 🚀 Phase 1N — Docker Deployment Guide

The entire SafeWalk stack can be containerized and run locally via Docker Compose.

### Prerequisites
- Docker Engine & Docker Compose (v2.x+ / Docker Desktop with WSL2 on Windows)
- JDK 17+ (if building locally outside Docker)
- Maven 3.9+ (if building locally outside Docker)

---

### Quick Start with Docker Compose

1. **Build the container images:**
   ```bash
   docker compose build
   ```

2. **Launch all services (MySQL, Redis, Kafka, Backend):**
   ```bash
   docker compose up -d
   ```

3. **Check container status & health:**
   ```bash
   docker compose ps
   ```

4. **View live application logs:**
   ```bash
   # All services
   docker compose logs -f

   # SafeWalk Spring Boot backend container only
   docker compose logs -f safewalk-backend
   ```

5. **Stop all services:**
   ```bash
   docker compose down
   ```

---

## ⚙️ Environment Variables & Configuration

Copy `.env.example` to `.env` to customize settings. Environment variables are automatically picked up by Docker Compose:

| Variable | Docker Default | Description |
| :--- | :--- | :--- |
| `DB_HOST` | `mysql` | MySQL service name inside Docker network |
| `DB_PORT` | `3306` | MySQL internal container port |
| `MYSQL_HOST_PORT` | `3307` | MySQL published host port (prevents port 3306 collision) |
| `DB_NAME` | `safewalk_db` | Database name |
| `DB_USERNAME` | `root` | Database username |
| `DB_PASSWORD` | *(required)* | Database password (configured via `.env`) |
| `REDIS_HOST` | `redis` | Redis service name inside Docker network |
| `REDIS_PORT` | `6379` | Redis port |
| `KAFKA_BOOTSTRAP_SERVERS` | `kafka:29092` | Kafka broker internal bootstrap servers |
| `JWT_SECRET` | *(required)* | 256-bit signing key for JWT tokens (minimum 32 characters, configured via `.env`) |
| `JWT_EXPIRATION_MS` | `86400000` | JWT token expiry (24 hours) |
| `MAPS_API_KEY` | *(empty)* | Optional Google Maps API key (uses Haversine local fallback if empty) |
| `MAPS_PROVIDER` | `google` | Maps API provider |
| `GOOGLE_APPLICATION_CREDENTIALS` | *(empty)* | Path to Firebase service account JSON key |
| `UPLOAD_DIR` | `/app/uploads` | Base directory for uploaded identity verification documents and avatars |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:3000` | Comma-separated list of allowed frontend origins |
| `ADMIN_BOOTSTRAP_ENABLED` | `false` | Set to true to initialize default admin account on startup |
| `SERVER_PORT` | `8080` | Spring Boot HTTP port |

---

## 🔐 Firebase FCM Credentials Strategy

- **DO NOT** place your Firebase private JSON credentials inside the Docker image or commit them to Git.
- To use live FCM push notifications in Docker, mount your local JSON key file into the container at runtime:
  ```yaml
  volumes:
    - /path/to/local/firebase-service-account.json:/app/config/firebase-service-account.json:ro
  environment:
    GOOGLE_APPLICATION_CREDENTIALS: /app/config/firebase-service-account.json
  ```
- If `GOOGLE_APPLICATION_CREDENTIALS` is not supplied, the backend runs gracefully in **DEV Simulation Mode**, logging push alerts without throwing exceptions.

---

## 🗺️ External Maps API Configuration

- Supply your Google Maps API key via `MAPS_API_KEY`.
- If no key is configured (`MAPS_API_KEY=`), `MapsServiceImpl` automatically falls back to the **Haversine formula** for distance & ETA calculation and local coordinate mapping for geocoding.

---

## 🔍 Service Verification & Inspection Commands

### 1. MySQL Container Verification
```bash
docker exec -it safewalk-mysql mysql -u root -p<your_db_password> -e "USE safewalk_db; SHOW TABLES; SELECT * FROM emergencies;"
```

### 2. Redis GEO Container Verification
```bash
docker exec -it safewalk-redis redis-cli ping
docker exec -it safewalk-redis redis-cli GEOPOS safewalk:users:locations user:1
```

### 3. Kafka Consumer & Topics Verification
```bash
docker exec -it safewalk-kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
```

### 4. Run Automated End-to-End Test Suite
```bash
# Run local Maven test suite (121 tests)
mvn clean test

# Run Docker-based REST/WebSocket/SOS lifecycle test script
powershell -ExecutionPolicy Bypass -File scratch/docker_functional_test.ps1
```

---

## 📁 Architecture Overview

```text
safewalk-backend (Spring Boot 3.3.4, Java 17)
  ├── Ports: 8080:8080
  └── Communicates via Docker Network:
        ├── mysql:3306 (Persistent Volume: mysql_data)
        ├── redis:6379 (Persistent Volume: redis_data)
        └── kafka:29092 (KRaft Mode)
```
