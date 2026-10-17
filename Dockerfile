# ---- build stage: has the JDK and Maven, gets thrown away ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
# fixes Windows line endings and a missing executable bit
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
# download dependencies first so this layer is cached until pom.xml changes
RUN ./mvnw -q dependency:go-offline

COPY src ./src
RUN ./mvnw -q package -DskipTests

# ---- run stage: JRE only, so the image is small ----
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd --system --no-create-home appuser
COPY --from=build /workspace/target/*.jar app.jar
USER appuser

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]