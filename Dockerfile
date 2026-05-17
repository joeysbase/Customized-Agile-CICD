# ---- build stage ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradlew settings.gradle ./
COPY gradle/ gradle/
COPY server/ server/
COPY client/ client/
COPY gradle/libs.versions.toml gradle/libs.versions.toml
RUN chmod +x gradlew && ./gradlew :server:jar --no-daemon
# ---- runtime stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN apt-get update && apt-get install -y git && rm -rf /var/lib/apt/lists/*
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser
COPY --from=build /app/server/build/libs/server-all.jar app.jar
RUN chown appuser:appgroup /app/app.jar
# USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
