# Build stage - Uses a Maven image to build the application
FROM maven:3.9.8-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy the pom.xml FROM THE BACKEND SUBDIRECTORY relative to the context (.)
COPY backend/pom.xml ./pom.xml

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy the backend source code FROM THE BACKEND SUBDIRECTORY relative to the context (.)
COPY backend/src ./src

# Build the application, skipping tests
RUN mvn package -DskipTests -B

# Runtime stage - Uses a slim JRE image
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the executable JAR from the builder stage's target directory
COPY --from=builder /app/target/backend-0.0.1-SNAPSHOT.jar /app/app.jar

# Expose the application port
EXPOSE 8080

# Define the command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

# Metadata
LABEL maintainer="Isabel Bosquet <ibosquet@uoc.edu>"
LABEL description="PetConnect Backend Service"