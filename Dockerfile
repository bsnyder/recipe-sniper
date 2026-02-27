FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

# Install Maven
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

# Copy pom.xml first for dependency caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy frontend sources
COPY frontend/ frontend/

# Copy Java sources
COPY src/ src/

# Build (includes frontend build via frontend-maven-plugin)
RUN mvn clean package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

# Create directories for H2 data and logs
RUN mkdir -p /app/data /app/logs

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
