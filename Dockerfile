# syntax=docker/dockerfile:1
# Multi-stage: compila con el JDK y ejecuta solo con el JRE, como usuario sin privilegios.

FROM eclipse-temurin:21.0.13_9-jdk-alpine AS compilacion
WORKDIR /workspace
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null
COPY src/main ./src/main
RUN ./gradlew --no-daemon bootJar -x test

FROM eclipse-temurin:21.0.13_9-jre-alpine
RUN addgroup -S inventario && adduser -S -G inventario -H inventario
WORKDIR /app
COPY --from=compilacion --chown=inventario:inventario /workspace/build/libs/inventario-api.jar ./app.jar
USER inventario
# «Fecha actual» de Bogotá en las validaciones; la JVM usa como máximo el 75 % de la memoria del contenedor.
ENV JAVA_TOOL_OPTIONS="-Duser.timezone=America/Bogota -XX:MaxRAMPercentage=75"
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -qO- "http://localhost:${SERVER_PORT}/actuator/health" > /dev/null || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
