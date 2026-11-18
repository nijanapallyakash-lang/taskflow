FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src src
RUN mvn -B -q package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd -r taskflow && mkdir -p /app/uploads && chown taskflow /app/uploads
COPY --from=build /app/target/taskflow-1.0.0.jar app.jar
USER taskflow
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
