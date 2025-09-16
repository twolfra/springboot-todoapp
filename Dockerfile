# Use a JDK to build the app
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests

# Use a smaller JRE image to run the app
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/target/todoapp-0.0.1-SNAPSHOT.jar app.jar

# Expose port
EXPOSE 8080

# Run the jar
ENTRYPOINT ["java","-jar","app.jar"]

# Health check using actuator
HEALTHCHECK --interval=30s --timeout=10s --start-period=15s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1
