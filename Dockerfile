FROM gradle:jdk21-alpine AS builder

WORKDIR /app
COPY build.gradle settings.gradle ./
COPY src src

# 로컬의 gradlew 대신 컨테이너 내장 gradle 명령어를 사용하여 빌드 (테스트 스킵)
RUN gradle clean build -x test

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
