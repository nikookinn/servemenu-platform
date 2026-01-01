FROM eclipse-temurin:21
WORKDIR /app
COPY build/libs/config-server.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]