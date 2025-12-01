FROM maven:3-eclipse-temurin-17-alpine AS build

WORKDIR /app

COPY . .

RUN mvn clean package

RUN mv target/cx_tracability_usecase-0.0.1-SNAPSHOT.jar target/app.jar

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=build /app/target/app.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]