# ---- Stage 1: build ---------------------------------------------------------
# A full Maven + JDK 21 image, used only to compile and package the jar.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src

# Tests are run separately (./mvnw test); skip them here to keep the build fast.
RUN mvn clean package -DskipTests

# ---- Stage 2: runtime -------------------------------------------------------
# A small JRE-only image. Maven, the JDK and the source code are left behind.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# OPENAI_API_KEY is NOT baked into the image - pass it at run time if needed:
#   docker run -p 8080:8080 -e OPENAI_API_KEY=... taskflow:latest
ENTRYPOINT ["java", "-jar", "app.jar"]
