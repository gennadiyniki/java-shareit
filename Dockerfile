FROM eclipse-temurin:21-jre-jammy
VOLUME /tmp
COPY target/*.jar app.jar
# Профиль по умолчанию - prod (гарантированно сработает!)
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "/app.jar"]
