# 第一階段：用 Maven 編譯出 JAR
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# 第二階段：只帶著 JAR 執行，image 較小
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/miniclinic-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]