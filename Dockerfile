FROM eclipse-temurin:21-jre-jammy
VOLUME /tmp
COPY target/*.jar app.jar
# Профиль по умолчанию - prod
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "/app.jar"]