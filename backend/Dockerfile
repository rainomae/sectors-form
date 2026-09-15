# Build stage: package the application with the Gradle wrapper. Build files are copied first so the
# dependency download is cached as its own layer and only re-runs when they change.
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY gradlew build.gradle settings.gradle ./
COPY gradle gradle
COPY config config
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies --quiet > /dev/null
COPY src src
RUN ./gradlew --no-daemon bootJar -x test

# Run stage: JRE only, non-root user.
FROM eclipse-temurin:25-jre
WORKDIR /opt/app
COPY --from=build /workspace/build/libs/app.jar app.jar
USER 65534
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
