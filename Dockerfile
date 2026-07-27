FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 3003
ENTRYPOINT ["java", "-Dspring.profiles.active=docker", "-jar", "app.jar"]