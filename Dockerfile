# --- Build stage ---
FROM eclipse-temurin:21.0.13_9-jdk-noble AS build
WORKDIR /src

# Download dependencies first so they are cached between code changes.
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src src
RUN ./mvnw -B -q package -DskipTests && cp target/*.jar /src/app.jar

# --- Runtime stage ---
FROM eclipse-temurin:21.0.13_9-jre-noble
WORKDIR /app

RUN groupadd --system flagman && useradd --system --gid flagman --no-create-home flagman
COPY --from=build --chown=flagman:flagman /src/app.jar app.jar
USER flagman

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
