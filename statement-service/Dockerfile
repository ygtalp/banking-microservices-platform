FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
RUN mkdir -p /app/statements
EXPOSE 8091
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=docker"]
