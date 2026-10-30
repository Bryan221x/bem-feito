FROM eclipse-temurin:25.0.4.1_1-jdk-noble AS build
WORKDIR /build
COPY backend/.mvn .mvn
COPY backend/mvnw backend/pom.xml ./
RUN chmod +x mvnw
COPY backend/src src
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:25.0.4.1_1-jre-noble
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
RUN groupadd --gid 10001 app && useradd --uid 10001 --gid app --no-create-home app \
    && mkdir -p /app /var/lib/bemfeito/uploads && chown -R app:app /app /var/lib/bemfeito
WORKDIR /app
COPY --from=build --chown=app:app /build/target/bem-feito-api-0.0.1-SNAPSHOT.jar app.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=65", "-Djava.awt.headless=true", "-jar", "/app/app.jar"]
