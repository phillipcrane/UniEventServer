# Build stage
FROM eclipse-temurin:25-jdk-alpine AS builder
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
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# Copy layers least-to-most frequently changed so app code doesn't bust dependency layers
COPY --from=builder /app/target/extracted/dependencies/ ./
COPY --from=builder /app/target/extracted/spring-boot-loader/ ./
COPY --from=builder /app/target/extracted/snapshot-dependencies/ ./
COPY --from=builder /app/target/extracted/application/ ./

EXPOSE 8080

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
