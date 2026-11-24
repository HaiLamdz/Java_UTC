# =====================================================================
# STAGE 1: BIÊN DỊCH DỰ ÁN BẰNG MAVEN & JDK 17 (Dùng cho Root Directory = /)
# =====================================================================
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY e_commerce_java/pom.xml .
RUN mvn dependency:go-offline -B

COPY e_commerce_java/src ./src
RUN mvn clean package -DskipTests -B

# =====================================================================
# STAGE 2: CHẠY ỨNG DỤNG VỚI JRE 17 TỐI ƯU RAM CHO RENDER FREE TIER (512MB)
# =====================================================================
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY --from=build /workspace/target/*.jar app.jar
RUN mkdir -p /app/uploads/previews /app/uploads/covers /app/data

ENV SPRING_PROFILES_ACTIVE=prod
ENV PORT=8080

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -XX:MaxRAMPercentage=72.0 -XX:+UseSerialGC -Dfile.encoding=UTF-8 -Dserver.port=${PORT:-8080} -jar app.jar"]

