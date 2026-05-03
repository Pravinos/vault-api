# Stage 1: Build
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /build

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src

# Fix permissions and build
RUN chmod +x mvnw && \
    ./mvnw clean package -DskipTests

# Rename to a known filename so we don't rely on wildcards
RUN cp /build/target/*.jar /build/target/app.jar

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

RUN useradd -m -u 1000 appuser

COPY --from=builder /build/target/app.jar app.jar
RUN chown appuser:appuser app.jar

USER appuser

# Render ignores EXPOSE and uses $PORT, but this documents intent
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]