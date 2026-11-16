
# Stage 1: Build React frontend
FROM node:22-alpine AS frontend-build

WORKDIR /frontend

COPY wildfire-frontend/package*.json ./
RUN npm ci

COPY wildfire-frontend/ .
RUN npm run build


# Stage 2: Build Spring Boot backend
FROM maven:3.9-eclipse-temurin-21 AS backend-build

WORKDIR /app

COPY wildfire/pom.xml .
COPY wildfire/src ./src

COPY --from=frontend-build /frontend/dist/ ./src/main/resources/static/

RUN mvn clean package -DskipTests


# Stage 3: Run the application
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=backend-build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-Xmx320m", "-jar", "app.jar"]
