# Java 17 JDK가 들어 있는 가벼운 베이스 이미지
FROM eclipse-temurin:17-jdk-alpine

# Gradle 빌드 결과 jar 파일 경로 (build/libs/*.jar)
ARG JAR_FILE=build/libs/*.jar

# jar를 컨테이너 안으로 복사
COPY ${JAR_FILE} app.jar

# 스프링 부트 실행
ENTRYPOINT ["java","-jar","/app.jar"]