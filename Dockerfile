FROM openjdk:11-jdk-slim

# Install Maven
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy project files
COPY pom.xml .
COPY src ./src

# Build application
RUN mvn clean package -DskipTests

# Expose port
EXPOSE 8080

# Run application with embedded Jetty
CMD ["mvn", "jetty:run", "-Djetty.host=0.0.0.0", "-Djetty.port=8080"]