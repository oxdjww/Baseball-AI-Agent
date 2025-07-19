# backend/Dockerfile

# 1) Build stage
FROM gradle:7.6.0-jdk17 AS builder
WORKDIR /app

# 프로젝트 내부 캐시 디렉터리 설정
ENV GRADLE_USER_HOME=/app/.gradle
RUN mkdir -p /app/.gradle

# Gradle Wrapper (Unix용) 복사
COPY gradlew ./
RUN chmod +x gradlew

# settings.gradle과 build.gradle 복사
COPY settings.gradle ./
COPY build.gradle ./

# gradle 디렉터리(Wrapper용) 복사
COPY gradle ./gradle

# 소스 코드 복사
COPY src ./src

# Wrapper로 빌드
RUN ./gradlew clean build -x test --no-daemon

# 2) Runtime stage
FROM openjdk:17-slim
WORKDIR /app

# 빌드 산출물(JAR) 복사
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

