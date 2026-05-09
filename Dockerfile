# Môi trường Java 21 siêu nhẹ
FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
