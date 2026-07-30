FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Se copia sólo el pom antes que el código: mientras las dependencias no cambien,
# Docker reutiliza esta capa y no vuelve a descargarlas en cada build.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
COPY --from=build /build/target/*.jar app.jar
USER app

ENV SPRING_PROFILES_ACTIVE=docker

EXPOSE 3003
# MaxRAMPercentage en lugar de un -Xmx fijo: la JVM ajusta el heap al límite de
# memoria del contenedor, que cambia según dónde se despliegue.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
