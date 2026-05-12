# 1. Build Stage
# Gradle 빌드를 수행하기 위해 JDK 21이 설치된 환경을 사용합니다.
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app
# 빌드 캐시 최적화를 위해 의존성 파일들만 먼저 복사
COPY gradle gradle
COPY gradlew build.gradle settings.gradle ./
# gradlew 실행 권한 부여
RUN chmod +x gradlew

# 소스 코드 복사 및 빌드 실행 (테스트 스킵으로 빌드 속도 향상)
COPY src src
RUN ./gradlew clean build -x test

# 2. Runtime Stage
# 실제 운영 환경에 배포될 경량화된 JRE 이미지를 사용합니다. (멀티 스테이지 빌드)
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
# 앞선 빌드 스테이지에서 생성된 뚱뚱한(Fat) JAR 파일만 복사합니다.
COPY --from=builder /app/build/libs/*.jar app.jar

# 컨테이너 포트 개방
EXPOSE 8080

# ENTRYPOINT에 메모리 최적화 플래그를 추가하여 JVM을 실행합니다.
ENTRYPOINT ["java", "-XX:+UseG1GC", "-Xms512m", "-Xmx512m", "-jar", "app.jar"]
