# ─── Stage 1: Build ───────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS build

WORKDIR /app

# Copy Gradle wrapper and build files first so dependency downloads are cached
# as a separate layer (only re-runs when build.gradle changes)
COPY gradle/ gradle/
COPY gradlew settings.gradle build.gradle ./

RUN chmod +x gradlew \
    && ./gradlew dependencies -q --no-daemon

# Copy source and build, skipping tests
COPY src/ src/
RUN ./gradlew bootJar -x test -q --no-daemon

# ─── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine AS runtime

WORKDIR /app

# Run as a non-root user
RUN addgroup -S fleet && adduser -S fleet -G fleet

COPY --from=build /app/build/libs/fleet-0.0.1-SNAPSHOT.jar app.jar

RUN chown fleet:fleet app.jar
USER fleet

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
