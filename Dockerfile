# ---------- Etapa 1: compilar ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Primero solo el pom: así Docker reutiliza las dependencias descargadas
# mientras el pom.xml no cambie
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Luego el código fuente
COPY src ./src
RUN mvn -B package -DskipTests

# ---------- Etapa 2: ejecutar ----------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Usuario sin privilegios (no correr como root)
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

COPY --from=build /app/target/*.jar app.jar

# Perfil de producción por defecto
ENV SPRING_PROFILES_ACTIVE=prod

# Ajustes de memoria para caber en los 512 MB del plan gratuito de Render
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -Xss512k -XX:TieredStopAtLevel=1"

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]