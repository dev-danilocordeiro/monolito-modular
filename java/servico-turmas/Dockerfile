# Contexto de build: a pasta java/ (veja o docker-compose.yml)
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
COPY servico-turmas servico-turmas
RUN ./gradlew :servico-turmas:bootJar --no-daemon -q

FROM eclipse-temurin:21-jre
# Usuário sem privilégios: a JVM não roda como root
RUN groupadd --system app && useradd --system --gid app --no-create-home --shell /usr/sbin/nologin app
# O jar fica com dono root (só leitura para o usuário app)
COPY --from=build /src/servico-turmas/build/libs/servico-turmas.jar /app.jar
EXPOSE 8080
USER app
ENTRYPOINT ["java", "-jar", "/app.jar"]
