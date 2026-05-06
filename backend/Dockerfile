FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
RUN apk add --no-cache dos2unix
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN dos2unix mvnw && chmod +x mvnw && ./mvnw dependency:go-offline -B
COPY src src
RUN ./mvnw package -DskipTests -B

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
