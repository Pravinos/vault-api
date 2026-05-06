# Stage 1: Build
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /build

# Install Maven
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

# Copy source
COPY pom.xml .
COPY src src

# Build with system Maven (no wrapper needed)
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Create non-root user
RUN useradd -m -u 1000 appuser

# Copy JAR (use wildcard or rename depending on your pom)
COPY --from=builder /build/target/*.jar app.jar
RUN chown appuser:appuser app.jar

USER appuser

# Render injects $PORT — make sure Spring Boot respects it
ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]