# Stage 1: Build the application
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Render uses port 10000 by default for web services
ENV PORT=10000
EXPOSE 10000

# Tell Docker to pull these specific API keys from Render
ENV CENTRO_KEY=${CENTRO_KEY}
ENV OPENWEATHER_KEY=${OPENWEATHER_KEY}

# Start the app but restrict memory usage so Render doesn't kill it
ENTRYPOINT ["java", "-Xmx256m", "-jar", "app.jar"]