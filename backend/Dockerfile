FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY src src
RUN --mount=type=cache,target=/root/.m2 chmod +x mvnw && ./mvnw package -DskipTests
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S catraca && adduser -S catraca -G catraca
USER catraca
WORKDIR /app
COPY --from=build /app/target/catraca-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
