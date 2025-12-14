FROM eclipse-temurin:21-jre-jammy
VOLUME /tmp
COPY target/*.jar app.jar
# ЯВНО указываем профиль как аргумент
ENTRYPOINT ["java", "-jar", "/app.jar", "--spring.profiles.active=prod"]
