# Build stage
FROM eclipse-temurin:25.0.3_9-jdk-alpine-3.23 AS builder
WORKDIR /app

# Copy Maven wrapper first (cached unless wrapper version changes)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Download dependencies (cached unless pom.xml changes)
RUN ./mvnw dependency:go-offline -B

# Build
COPY src src
RUN ./mvnw package -DskipTests -B

# Rename to fixed name then extract layered jar for optimal Docker layer caching
RUN cp target/*.jar target/app.jar && \
    java -Djarmode=tools -jar target/app.jar extract --layers --destination target/extracted

# Runtime stage
FROM eclipse-temurin:25.0.3_9-jre-alpine
WORKDIR /app

# Copy layers least-to-most frequently changed so app code doesn't bust dependency layers
COPY --from=builder /app/target/extracted/dependencies/ ./
COPY --from=builder /app/target/extracted/snapshot-dependencies/ ./
COPY --from=builder /app/target/extracted/application/ ./

RUN addgroup -S app && adduser -S app -G app && \
    mkdir -p /app/logs && chown app:app /app/logs
USER app

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
