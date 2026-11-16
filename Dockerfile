# ==============================================================================
# Etapa 1: Construcción (Build Stage)
# ==============================================================================
FROM maven:3.9.9-eclipse-temurin-17 AS builder

WORKDIR /build

# 1. Copiar el descriptor del proyecto para aprovechar el caché de capas de Docker
COPY pom.xml .

# 2. Pre-descargar dependencias (acelera despliegues posteriores si pom.xml no cambia)
RUN mvn dependency:go-offline -B || true

# 3. Copiar el código fuente y compilar el paquete omitiendo pruebas para despliegue
COPY src ./src
RUN mvn clean package -DskipTests -B && \
    cp target/*.jar app.jar

# ==============================================================================
# Etapa 2: Imagen Final de Ejecución Ligera (Runtime Stage)
# ==============================================================================
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Crear usuario y grupo sin privilegios de root por seguridad
RUN groupadd -r spring && useradd -r -g spring spring

# Copiar el ejecutable JAR desde la etapa previa
COPY --from=builder /build/app.jar app.jar

# Asignar permisos al usuario de la aplicación
RUN chown -R spring:spring /app

USER spring:spring

# Puerto expuesto por el contenedor
EXPOSE 8080

# Opciones de JVM optimizadas para contenedores Docker (respeta límites de memoria de EasyPanel)
ENV PORT=8080 \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Punto de entrada flexible: toma automáticamente el puerto asignado por EasyPanel o variables de entorno
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT} -jar app.jar"]

