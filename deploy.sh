#!/bin/bash

# ShopSite 部署脚本
# 使用方法: ./deploy.sh

set -e  # 遇到错误立即退出

echo "=========================================="
echo "ShopSite 部署脚本"
echo "=========================================="

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查是否在项目根目录
if [ ! -f "pom.xml" ]; then
    echo -e "${RED}错误: 请在项目根目录运行此脚本${NC}"
    exit 1
fi

echo -e "${YELLOW}步骤 1: 清理并构建项目...${NC}"
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo -e "${RED}构建失败！${NC}"
    exit 1
fi

echo -e "${GREEN}构建成功！${NC}"

# 检查 JAR 文件是否存在
JAR_FILE="target/shopsite-0.0.1-SNAPSHOT.jar"
if [ ! -f "$JAR_FILE" ]; then
    echo -e "${RED}错误: 找不到 JAR 文件: $JAR_FILE${NC}"
    exit 1
fi

echo -e "${YELLOW}步骤 2: 准备部署文件...${NC}"

# 创建部署目录
DEPLOY_DIR="deploy"
mkdir -p $DEPLOY_DIR

# 复制 JAR 文件
cp $JAR_FILE $DEPLOY_DIR/shopsite.jar

# 复制配置文件模板
if [ ! -f "$DEPLOY_DIR/application-prod.yml" ]; then
    echo -e "${YELLOW}创建生产环境配置文件模板...${NC}"
    cat > $DEPLOY_DIR/application-prod.yml << 'EOF'
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/shopsite_db?useSSL=true&serverTimezone=Asia/Shanghai
    username: shopsite_user
    password: YOUR_PASSWORD_HERE
    driver-class-name: com.mysql.cj.jdbc.Driver

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false

  servlet:
    multipart:
      enabled: true
      max-file-size: 10MB
      max-request-size: 10MB

file:
  upload:
    dir: /var/www/shopsite/uploads/

logging:
  level:
    root: INFO
    com.example.shopsite: INFO
  file:
    name: /var/log/shopsite/application.log
EOF
fi

# 创建 systemd 服务文件
cat > $DEPLOY_DIR/shopsite.service << 'EOF'
[Unit]
Description=ShopSite Application
After=network.target mysql.service

[Service]
Type=simple
User=shopsite
WorkingDirectory=/var/www/shopsite
Environment="SPRING_PROFILES_ACTIVE=prod"
Environment="JAVA_OPTS=-Xms512m -Xmx1024m -XX:+UseG1GC"
ExecStart=/usr/bin/java $JAVA_OPTS -jar /var/www/shopsite/shopsite.jar
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=shopsite

[Install]
WantedBy=multi-user.target
EOF

# 创建 Nginx 配置文件
cat > $DEPLOY_DIR/shopsite-nginx.conf << 'EOF'
server {
    listen 80;
    server_name your-domain.com www.your-domain.com;

    client_max_body_size 10M;

    location ~* \.(jpg|jpeg|png|gif|ico|css|js|woff|woff2|ttf|svg)$ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        expires 30d;
        add_header Cache-Control "public, immutable";
    }

    location /uploads/ {
        alias /var/www/shopsite/uploads/;
        expires 7d;
        add_header Cache-Control "public";
    }

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }
}
EOF

# 创建部署说明
cat > $DEPLOY_DIR/README.txt << 'EOF'
部署文件说明
=============

1. shopsite.jar - 应用程序 JAR 文件
2. application-prod.yml - 生产环境配置文件（需要修改数据库密码）
3. shopsite.service - systemd 服务文件
4. shopsite-nginx.conf - Nginx 配置文件

部署步骤：
1. 上传 shopsite.jar 到服务器 /var/www/shopsite/
2. 修改 application-prod.yml 中的数据库密码
3. 将 application-prod.yml 放到 /var/www/shopsite/
4. 将 shopsite.service 复制到 /etc/systemd/system/
5. 将 shopsite-nginx.conf 复制到 /etc/nginx/conf.d/
6. 按照 DEPLOYMENT.md 中的说明完成部署

详细说明请查看 DEPLOYMENT.md
EOF

echo -e "${GREEN}部署文件已准备完成！${NC}"
echo ""
echo -e "${YELLOW}部署文件位置: $DEPLOY_DIR/${NC}"
echo ""
echo "下一步："
echo "1. 查看 $DEPLOY_DIR/README.txt 了解部署步骤"
echo "2. 查看 DEPLOYMENT.md 了解详细部署指南"
echo "3. 将 $DEPLOY_DIR 目录中的文件上传到服务器"



