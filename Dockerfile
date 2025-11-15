# ---- Build stage ----
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

# Copy Gradle wrapper & build scripts
COPY build.gradle.kts settings.gradle.kts gradlew ./
COPY gradle ./gradle

# Download dependencies
RUN ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

# Copy sources
COPY src ./src

# Build the jar
RUN ./gradlew --no-daemon bootJar

# ---- Run stage ----
FROM eclipse-temurin:21-jdk AS runtime

WORKDIR /app

# Copy built jar
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]