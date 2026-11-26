# ---------- Etapa 1: compilar ----------
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
# Primero solo el pom: Docker reutiliza esta capa (dependencias) mientras el pom no cambie.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
# Las pruebas se corren aparte (mvnw test); aquí solo se empaqueta.
RUN mvn -B -q package -DskipTests

# ---------- Etapa 2: ejecutar ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /app/target/parcial-api.jar app.jar
USER app
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
HEALTHCHECK --interval=10s --timeout=3s --start-period=40s --retries=5 \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
