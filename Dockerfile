FROM eclipse-temurin:18-jdk-alpine

WORKDIR /app

COPY target/fraudlens-api-*.jar app.jar

EXPOSE 8080

# Einfach JAR starten - Env Vars werden automatisch gelesen!
ENTRYPOINT ["java", "-jar", "app.jar"]