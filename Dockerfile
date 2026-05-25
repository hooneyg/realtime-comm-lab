# ╔══════════════════════════════════════════════════════════════════╗
# ║         🐳 Dockerfile (Multi-stage Build for Java 21)           ║
# ║                                                                  ║
# ║  [빌드 전략]                                                     ║
# ║  빌드 환경과 실행 환경을 분리하여 공격 표면을 줄이고 이미지 크기 최적화 ║
# ╚══════════════════════════════════════════════════════════════════╝

# --- Step 1: Build Stage (컴파일 단계) ---
FROM gradle:jdk21-alpine AS builder

WORKDIR /app
COPY build.gradle settings.gradle ./
COPY gradle gradle
COPY gradlew ./
COPY src src

# [추가] alpine 환경에 dos2unix 설치 후 gradlew 파일의 줄바꿈을 LF로 강제 변환
RUN apk add --no-cache dos2unix && dos2unix gradlew

# Gradle Wrapper에 실행 권한 부여 및 빌드 (테스트 스킵)
RUN chmod +x gradlew && ./gradlew clean build -x test

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
