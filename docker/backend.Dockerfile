# syntax=docker/dockerfile:1.7
FROM maven:3.9.16-eclipse-temurin-25 AS build

ARG SERVICE
WORKDIR /workspace

COPY apps/${SERVICE}/backend/pom.xml ./pom.xml
RUN --mount=type=cache,id=marketplace-maven,target=/root/.m2,sharing=locked \
    mvn -B -DskipTests dependency:go-offline

COPY apps/${SERVICE}/backend/src ./src
RUN --mount=type=cache,id=marketplace-maven,target=/root/.m2,sharing=locked \
    mvn -B -DskipTests package

FROM eclipse-temurin:25-jre-noble

RUN groupadd --gid 10001 marketplace \
    && useradd --uid 10001 --gid marketplace --home-dir /app --no-create-home --shell /usr/sbin/nologin marketplace
WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar

USER 10001:10001
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
