# Giai đoạn 1: Dùng Maven và Java 21 để build ra file .jar
FROM maven:3-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Giai đoạn 2: Môi trường Java 21 siêu nhẹ (như bạn đã chọn) để chạy app
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
# Copy file .jar đã được build thành công từ giai đoạn 1 sang đây
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
