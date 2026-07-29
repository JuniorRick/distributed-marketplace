FROM maven:3.9.16-eclipse-temurin-25 AS build

ARG SERVICE
WORKDIR /workspace

COPY apps/${SERVICE}/backend/pom.xml ./pom.xml
RUN mvn -B -DskipTests dependency:go-offline

COPY apps/${SERVICE}/backend/src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:25-jre-noble

RUN useradd --system --create-home marketplace
WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar

USER marketplace
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
