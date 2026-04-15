FROM amazoncorretto:24
VOLUME /tmp
COPY build/libs/*.jar app.jar

# Run with azure profile by default, Liquibase will auto-initialize database schema
ENTRYPOINT ["java", "-jar", "/app.jar", "--spring.profiles.active=azure"]
EXPOSE 8080