# 阿里云服务器部署指南

本文档详细说明如何将 ShopSite 电商网站部署到阿里云服务器。

## 📋 目录

1. [服务器准备](#服务器准备)
2. [环境配置](#环境配置)
3. [数据库配置](#数据库配置)
4. [应用部署](#应用部署)
5. [Nginx 配置](#nginx-配置)
6. [SSL 证书配置](#ssl-证书配置)
7. [监控和维护](#监控和维护)
8. [常见问题](#常见问题)

---

## 🖥️ 服务器准备

### 1. 购买阿里云 ECS 实例

- **推荐配置**：
  - CPU: 2核
  - 内存: 4GB
  - 系统盘: 40GB SSD
  - 操作系统: CentOS 7.9 / Ubuntu 20.04
  - 带宽: 3Mbps 起步

### 2. 安全组配置

在阿里云控制台配置安全组规则：

```
入方向规则：
- HTTP (80)    - 允许所有IP
- HTTPS (443)  - 允许所有IP
- SSH (22)     - 仅允许您的IP（安全考虑）
- 自定义TCP (8080) - 仅允许内网访问（用于Nginx反向代理）
```

---

## ⚙️ 环境配置

### 1. 连接到服务器

```bash
ssh root@your-server-ip
```

### 2. 更新系统

**CentOS:**
```bash
sudo yum update -y
```

**Ubuntu:**
```bash
sudo apt update && sudo apt upgrade -y
```

### 3. 安装 Java 17

**CentOS:**
```bash
sudo yum install -y java-17-openjdk java-17-openjdk-devel
```

**Ubuntu:**
```bash
sudo apt install -y openjdk-17-jdk
```

验证安装：
```bash
java -version
```

### 4. 安装 MySQL 8.0

**CentOS:**
```bash
# 安装 MySQL 仓库
sudo yum install -y https://dev.mysql.com/get/mysql80-community-release-el7-3.noarch.rpm
sudo yum install -y mysql-server
sudo systemctl start mysqld
sudo systemctl enable mysqld
```

**Ubuntu:**
```bash
sudo apt install -y mysql-server
sudo systemctl start mysql
sudo systemctl enable mysql
```

**初始化 MySQL:**
```bash
sudo mysql_secure_installation
```

**创建数据库和用户:**
```bash
sudo mysql -u root -p
```

```sql
CREATE DATABASE shopsite_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'shopsite_user'@'localhost' IDENTIFIED BY 'your_secure_password';
GRANT ALL PRIVILEGES ON shopsite_db.* TO 'shopsite_user'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

### 5. 安装 Nginx

**CentOS:**
```bash
sudo yum install -y nginx
```

**Ubuntu:**
```bash
sudo apt install -y nginx
```

启动 Nginx:
```bash
sudo systemctl start nginx
sudo systemctl enable nginx
```

---

## 🗄️ 数据库配置

### 选项A: 使用本地 MySQL（适合小型项目）

已在上一步完成配置。

### 选项B: 使用阿里云 RDS（推荐生产环境）

1. 在阿里云控制台创建 RDS MySQL 实例
2. 配置白名单（添加 ECS 内网IP）
3. 创建数据库和用户
4. 记录连接信息：
   - 内网地址（推荐）
   - 外网地址（如果需要）
   - 用户名和密码

---

## 🚀 应用部署

### 1. 创建应用用户和目录

```bash
# 创建应用用户
sudo useradd -m -s /bin/bash shopsite

# 创建应用目录
sudo mkdir -p /var/www/shopsite
sudo mkdir -p /var/log/shopsite
sudo mkdir -p /var/www/shopsite/uploads

# 设置权限
sudo chown -R shopsite:shopsite /var/www/shopsite
sudo chown -R shopsite:shopsite /var/log/shopsite
```

### 2. 上传应用文件

在本地构建项目：
```bash
mvn clean package -DskipTests
```

上传到服务器：
```bash
scp target/shopsite-0.0.1-SNAPSHOT.jar shopsite@your-server-ip:/var/www/shopsite/shopsite.jar
```

### 3. 创建配置文件

在服务器上创建 `/var/www/shopsite/application-prod.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/shopsite_db?useSSL=true&serverTimezone=Asia/Shanghai
    username: shopsite_user
    password: your_secure_password
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
```

### 4. 创建 systemd 服务

创建 `/etc/systemd/system/shopsite.service`:

```ini
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
```

启动服务：
```bash
sudo systemctl daemon-reload
sudo systemctl enable shopsite
sudo systemctl start shopsite
sudo systemctl status shopsite
```

查看日志：
```bash
sudo journalctl -u shopsite -f
```

---

## 🌐 Nginx 配置

### 1. 创建 Nginx 配置文件

创建 `/etc/nginx/conf.d/shopsite.conf`:

```nginx
server {
    listen 80;
    server_name your-domain.com www.your-domain.com;

    # 文件上传大小限制
    client_max_body_size 10M;

    # 静态资源缓存
    location ~* \.(jpg|jpeg|png|gif|ico|css|js|woff|woff2|ttf|svg)$ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        expires 30d;
        add_header Cache-Control "public, immutable";
    }

    # 上传文件访问
    location /uploads/ {
        alias /var/www/shopsite/uploads/;
        expires 7d;
        add_header Cache-Control "public";
    }

    # 应用代理
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
```

测试配置：
```bash
sudo nginx -t
```

重载 Nginx:
```bash
sudo systemctl reload nginx
```

---

## 🔒 SSL 证书配置

### 使用 Let's Encrypt 免费证书

**CentOS:**
```bash
sudo yum install -y certbot python3-certbot-nginx
```

**Ubuntu:**
```bash
sudo apt install -y certbot python3-certbot-nginx
```

申请证书：
```bash
sudo certbot --nginx -d your-domain.com -d www.your-domain.com
```

证书会自动续期，Nginx 配置会自动更新。

---

## 📊 监控和维护

### 1. 日志管理

查看应用日志：
```bash
tail -f /var/log/shopsite/application.log
```

查看系统日志：
```bash
sudo journalctl -u shopsite -f
```

### 2. 配置日志轮转

创建 `/etc/logrotate.d/shopsite`:

```
/var/log/shopsite/*.log {
    daily
    rotate 30
    compress
    delaycompress
    missingok
    notifempty
    create 0644 shopsite shopsite
    sharedscripts
    postrotate
        systemctl reload shopsite > /dev/null 2>&1 || true
    endscript
}
```

### 3. 数据库备份

创建备份脚本 `/var/www/shopsite/backup-db.sh`:

```bash
#!/bin/bash
BACKUP_DIR="/var/www/shopsite/backups"
DATE=$(date +%Y%m%d_%H%M%S)
mkdir -p $BACKUP_DIR

mysqldump -u shopsite_user -p'your_password' shopsite_db > $BACKUP_DIR/shopsite_db_$DATE.sql

# 删除7天前的备份
find $BACKUP_DIR -name "*.sql" -mtime +7 -delete
```

设置定时任务（每天凌晨2点备份）：
```bash
crontab -e
# 添加：
0 2 * * * /var/www/shopsite/backup-db.sh
```

### 4. 文件备份

```bash
# 备份上传的文件
tar -czf /var/www/shopsite/backups/uploads_$(date +%Y%m%d).tar.gz /var/www/shopsite/uploads/
```

---

## 🔄 更新部署

### 部署脚本

创建 `/var/www/shopsite/deploy.sh`:

```bash
#!/bin/bash

echo "开始部署..."

# 1. 停止服务
sudo systemctl stop shopsite

# 2. 备份旧版本
if [ -f /var/www/shopsite/shopsite.jar ]; then
    cp /var/www/shopsite/shopsite.jar /var/www/shopsite/shopsite.jar.backup.$(date +%Y%m%d_%H%M%S)
fi

# 3. 复制新版本（需要先上传新文件）
# scp target/shopsite.jar shopsite@server:/var/www/shopsite/shopsite.jar.new
# mv /var/www/shopsite/shopsite.jar.new /var/www/shopsite/shopsite.jar

# 4. 启动服务
sudo systemctl start shopsite

# 5. 检查状态
sleep 5
sudo systemctl status shopsite

echo "部署完成！"
```

---

## ❓ 常见问题

### 1. 应用无法启动

检查日志：
```bash
sudo journalctl -u shopsite -n 50
```

检查端口占用：
```bash
sudo netstat -tlnp | grep 8080
```

### 2. 数据库连接失败

检查 MySQL 服务：
```bash
sudo systemctl status mysql
```

测试连接：
```bash
mysql -u shopsite_user -p shopsite_db
```

### 3. 文件上传失败

检查目录权限：
```bash
ls -la /var/www/shopsite/uploads/
sudo chown -R shopsite:shopsite /var/www/shopsite/uploads/
```

### 4. Nginx 502 错误

检查应用是否运行：
```bash
sudo systemctl status shopsite
curl http://localhost:8080
```

---

## 📝 检查清单

部署前确认：

- [ ] 服务器已购买并配置安全组
- [ ] Java 17 已安装
- [ ] MySQL 已安装并创建数据库
- [ ] 应用已构建并上传
- [ ] 配置文件已创建
- [ ] systemd 服务已配置
- [ ] Nginx 已配置并运行
- [ ] 域名已解析到服务器IP
- [ ] SSL 证书已配置（可选但推荐）
- [ ] 防火墙规则已配置
- [ ] 备份脚本已设置

---

## 🎯 上线步骤总结

1. **准备服务器**：购买 ECS，配置安全组
2. **安装环境**：Java、MySQL、Nginx
3. **配置数据库**：创建数据库和用户
4. **部署应用**：上传 JAR 包，配置 systemd
5. **配置 Nginx**：反向代理和静态资源
6. **配置域名**：DNS 解析到服务器
7. **配置 SSL**：申请并安装证书
8. **测试验证**：检查所有功能
9. **设置监控**：日志和备份

完成以上步骤后，您的网站就可以正式上线了！🎉



