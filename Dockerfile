#FROM ubuntu:latest

#ENTRYPOINT ["top", "-b"]
# 1: Build the application JAR using Gradle and JDK 23
# Use an official Gradle image that includes JDK 23. Adjust the tag if needed.
# Check Docker Hub for available gradle:jdk23 tags (e.g., gradle:8.8.0-jdk23)

FROM gradle:8.13.0-jdk23 AS build

# Set the working directory inside the container
WORKDIR /app

# Copy the Gradle wrapper files
COPY gradlew .
COPY gradle gradle

# Copy the build configuration files
COPY build.gradle.kts .
# Copy settings.gradle.

#kts if you have one
COPY settings.gradle.kts .

# Copy the source code
COPY src src

# Grant execution rights to the Gradle wrapper
RUN chmod +x ./gradlew

# Build the application, creating the executable JAR
# Use --no-daemon to prevent the Gradle daemon from running

 #in the container
RUN ./gradlew bootJar --no-daemon

# Stage 2: Create the final runtime image using a JRE
# Use a minimal JRE image compatible with Java 23 (e.g., Eclipse Temurin)
# Alpine versions are smaller but might have compatibility issues with some
# native libraries.
# Consider eclipse-temurin:23-jre for a standard glibc-based image if Alpine causes issues.
FROM eclipse-temurin:23-jre-alpine

# Set the working directory
WORKDIR /app

# Copy the executable JAR from the build stage
# Adjust the JAR name if your build.gradle.kts produces a different name
# It's typically found in build/libs/your-app-name-version.jar
COPY --from=build /app/build/libs/*.jar app.jar

# Expose the port the application runs on (default for Spring Boot is 8080)
EXPOSE 8080

# Set the entry point to run the application
# java -jar will execute the JAR file
ENTRYPOINT ["java", "-jar", "app.jar"]

# Optional: Add labels for metadata (good practice)
LABEL description="dockerlogger"
LABEL author="zr0am"
