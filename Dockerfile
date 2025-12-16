
FROM eclipse-temurin:21-jdk-jammy

# 设置工作目录
WORKDIR /app

# 将本地编译好的 JAR 文件复制到容器中
ARG JAR_FILE=shopsite-0.0.1-SNAPSHOT.jar
COPY target/${JAR_FILE} /app/app.jar

# 暴露 Spring Boot 应用的端口
EXPOSE 8080

# 容器启动命令：激活 prod profile，连接到 Docker 网络内的 shopsite-mysql
ENTRYPOINT ["java", "-jar", "app.jar"]