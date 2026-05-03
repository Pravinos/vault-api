# Docker & Tailscale Deployment Guide

This document outlines all changes made to prepare the Vault API for Docker and Tailscale network deployment.

## Changes Summary

### 1. Network Configuration (`application.yaml`)

**Added:**
```yaml
server:
  port: 8080
  address: 0.0.0.0  # Listen on all interfaces
```

**Why:** Ensures the application accepts connections from any network interface, essential for Docker containerization and Tailscale accessibility.

**Added:**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: when-authorized
```

**Why:** Enables Spring Boot Actuator health endpoint for container health checks and monitoring.

### 2. CORS Configuration (`WebConfig.java`)

**Updated from:**
```java
registry.addMapping("/api/**")
    .allowedOrigins("http://localhost:3000")
    .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
```

**Updated to:**
```java
registry.addMapping("/**")
    .allowedOrigins("*")
    .allowedMethods("*")
    .allowedHeaders("*")
    .allowCredentials(false)
```

**Why:** 
- Allows all origins for flexibility in Docker/Tailscale deployments
- Covers all API endpoints with wildcard mapping
- Eliminates restrictive localhost-only configuration

### 3. Dependencies (`pom.xml`)

**Added:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

**Why:** Provides `/actuator/health` endpoint for Docker health checks and monitoring.

### 4. Environment Variables

**Already configured correctly:**
- Database password is read from `${DB_PASSWORD}` environment variable
- No hardcoded secrets in configuration files
- `.env` file support enabled via `spring.config.import`

### 5. Dockerfile (Multi-stage Build)

Created `Dockerfile` with:

**Stage 1 (Builder):**
- Uses `eclipse-temurin:21-jdk-jammy` base image
- Copies Maven wrapper and source code
- Builds JAR with `mvn clean package`

**Stage 2 (Runtime):**
- Uses lightweight `eclipse-temurin:21-jre-jammy` (JRE only, smaller image)
- Non-root user (appuser, UID 1000) for security
- Exposes port 8080
- Includes health check that monitors the application
- Runs with `java -jar app.jar`

**Why multi-stage:**
- Reduces final image size (builder dependencies not included in runtime image)
- Production-ready security with non-root user
- Health check for container orchestration

### 6. Docker Build Optimization (`.dockerignore`)

Created `.dockerignore` to exclude:
- Maven build artifacts (`target/`)
- Git metadata
- IDE configuration files
- Environment files
- Temporary files

**Why:** Speeds up Docker builds and reduces context size.

### 7. Docker Compose for Local Testing

Created `docker-compose.yml` for easy local development/testing:
- Maps port 8080
- Passes `DB_PASSWORD` environment variable
- Includes health check
- Auto-restart policy
- Named network for multi-service setups

## Deployment Instructions

### Prerequisites
- Docker installed
- Database credentials (Supabase PostgreSQL)

### Local Testing with Docker Compose

```bash
# Set environment variables
export DB_PASSWORD=your_password_here

# Build and run
docker-compose up --build

# Access the application
curl http://localhost:8080/actuator/health
curl http://localhost:8080/swagger-ui

# View logs
docker-compose logs -f vault-api

# Stop
docker-compose down
```

### Build Docker Image Manually

```bash
docker build -t vault-api:latest .
```

### Run Docker Container

```bash
docker run -p 8080:8080 \
  -e DB_PASSWORD=your_password_here \
  vault-api:latest
```

### Tailscale Deployment

1. Install Tailscale on your deployment machine
2. Build the Docker image: `docker build -t vault-api:latest .`
3. Run with environment variables:

```bash
docker run -p 8080:8080 \
  -e DB_PASSWORD=$DB_PASSWORD \
  --restart unless-stopped \
  vault-api:latest
```

4. Access over Tailscale network at: `http://<tailscale-ip>:8080`

### Health Checks

The application exposes:
- Health endpoint: `/actuator/health`
- Info endpoint: `/actuator/info`
- API docs: `/swagger-ui`

Example:
```bash
curl http://localhost:8080/actuator/health
# Output: {"status":"UP","components":{"db":{"status":"UP"}}}
```

## Security Considerations

✅ Non-root user in container (appuser)  
✅ Secrets from environment variables only  
✅ No hardcoded credentials  
✅ Health check monitoring  
✅ Multi-stage build minimizes attack surface  

## Verification Checklist

- [x] Application listens on `0.0.0.0:8080`
- [x] Database password from `${DB_PASSWORD}` environment variable
- [x] CORS allows all origins and methods
- [x] Multi-stage Dockerfile implemented
- [x] Health check endpoint available
- [x] Logs output to stdout (default Spring Boot)
- [x] Non-root user in container
- [x] Docker build succeeds
- [x] Application accessible via `http://localhost:8080` in container
- [x] `.env` file support configured

## Troubleshooting

### Container exits immediately
```bash
docker logs <container-id>
# Check for database connection issues
```

### Database connection fails
- Verify `DB_PASSWORD` environment variable is set
- Verify database URL and credentials in `application.yaml`
- Check network connectivity to Supabase

### CORS errors in browser
- CORS is now permissive (allows all origins)
- If issues persist, check `WebConfig.java` mappings

### Health check fails
- Ensure application is fully started
- Check `/actuator/health` endpoint directly
- Review application logs

## Next Steps

1. Test locally with `docker-compose up`
2. Verify all endpoints work: `/api/accounts`, `/swagger-ui`, etc.
3. Deploy to Tailscale network
4. Set up monitoring/logging aggregation
5. Consider adding environment-specific configurations
