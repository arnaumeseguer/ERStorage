# ---------- stage 1: build the jar ----------
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

COPY src src
RUN ./mvnw -B package -DskipTests

# ---------- stage 2: run the jar ----------
FROM eclipse-temurin:25-jre
WORKDIR /app

RUN useradd --system --create-home appuser
COPY --from=build /app/target/*.jar app.jar
USER appuser

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseCompactObjectHeaders -XX:+UseSerialGC -Xss512k"
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]