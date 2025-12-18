# ShopSite 电商平台

一个基于 Spring Boot 4.0 开发的全功能电商平台，支持多角色管理（用户、商家、管理员），提供完整的购物流程、订单管理和商家后台功能。

## 1. 项目简介

ShopSite 是一个现代化的电商平台系统，采用前后端分离的设计理念，后端使用 Spring Boot 构建 RESTful API，前端使用 Thymeleaf 模板引擎渲染页面。系统支持三种角色：普通用户（购物）、商家（商品管理）、管理员（平台管理）。

## 2. 功能特性

### 用户端功能

- 用户注册与登录（基于 Spring Security）
- 商品浏览与搜索
- 商品分类筛选
- 购物车管理
- 商品收藏功能
- 订单创建与查询
- 个人资料管理
- 订单状态跟踪

### 商家端功能

- 商品管理（增删改查）
- 商品上架/下架
- 订单处理（发货、状态更新）
- 销售统计与报表
- 客户管理
- 销售日志记录

### 管理员端功能

- 商家管理（查看、审核）
- 商品审核与管理
- 订单监控
- 系统数据统计
- 平台管理

## 3. 技术栈

### 后端框架

- **Spring Boot** 4.0.0
- **Spring MVC** - Web 层框架
- **Spring Data JPA** - 数据持久化
- **Spring Security** - 安全认证
- **Spring Validation** - 数据校验
- **Spring Mail** - 邮件服务

### 数据库

- **MySQL** 8.0
- **Hibernate** - ORM 框架

### 前端技术

- **Thymeleaf** - 服务端模板引擎
- **Bootstrap** 5.x - CSS 框架
- **JavaScript** - 前端交互

### 开发工具

- **Maven** - 项目构建工具
- **Lombok** - 代码简化
- **Docker** - 容器化部署
- **Docker Compose** - 多容器编排

### 其他

- **Java** 21 (LTS)
- **JWT** - 身份认证（可选）
- **BCrypt** - 密码加密

## 4. 项目结构

```
shopsite/
├── src/
│ ├── main/
│ │ ├── java/com/example/shopsite/
│ │ │ ├── config/ # 配置类（安全、初始化等）
│ │ │ ├── controller/ # 控制器层
│ │ │ │ ├── admin/ # 管理员控制器
│ │ │ │ ├── merchant/ # 商家控制器
│ │ │ │ └── user/ # 用户控制器
│ │ │ ├── dto/ # 数据传输对象
│ │ │ ├── exception/ # 异常类
│ │ │ ├── handler/ # 异常处理器
│ │ │ ├── interceptor/ # 拦截器
│ │ │ ├── model/ # 实体类（User, Product, Order等）
│ │ │ ├── repository/ # 数据访问层
│ │ │ ├── security/ # 安全配置
│ │ │ ├── service/ # 业务逻辑层
│ │ │ │ └── impl/ # 服务实现类
│ │ │ └── ShopsiteApplication.java
│ │ └── resources/
│ │ ├── application.yml # 主配置文件
│ │ ├── application-dev.yml
│ │ ├── application-prod.yml
│ │ ├── db/
│ │ │ └── init.sql # 数据库初始化脚本
│ │ ├── static/ # 静态资源（CSS、JS、图片）
│ │ └── templates/ # Thymeleaf 模板
│ └── test/ # 测试代码
├── uploads/ # 文件上传目录
├── docker-compose.yml # Docker Compose 配置
├── Dockerfile # Docker 镜像构建文件
├── pom.xml # Maven 配置文件
└── README.md # 项目说明文档
```

## 5. 环境要求

### 必需环境

- **JDK** 21 或更高版本
- **Maven** 3.6+（或使用项目自带的 Maven Wrapper）
- **MySQL** 8.0
- **Docker** 和 **Docker Compose**（推荐用于快速部署）

### 可选工具

- **IntelliJ IDEA** / **Eclipse** / **VS Code**（IDE）
- **Postman** / **Thunder Client**（API 测试）
- **MySQL Workbench** / **Navicat**（数据库管理）

## 6. 快速开始

### 方式一：Docker Compose 部署（推荐）

#### 1. 克隆项目

`git clone <项目地址>
cd shopsite`

#### 2. 创建环境配置文件

在项目根目录创建 `.env` 文件：

##### MySQL 数据库配置

```
MYSQL_ROOT_PASSWORD=root123
MYSQL_DATABASE=shopsite_db
MYSQL_USER=shopuser
MYSQL_PASSWORD=shoppass123
```

##### 应用数据库连接配置

```
DB_USERNAME=shopuser
DB_PASSWORD=shoppass123
```

##### 生产环境数据库连接

```
PROD_DB_URL=jdbc:mysql://shopsite-mysql:3306/shopsite_db?useSSL=false&serverTimezone=UTC
PROD_DB_USERNAME=shopuser
PROD_DB_PASSWORD=shoppass123
```

#### 3. 构建项目

**Windows**

`.\mvnw clean package -DskipTests`

**Linux/Mac**

`./mvnw clean package -DskipTests`

#### 4. 启动服务

`docker-compose up -d`

#### 5. 查看日志

###### 查看应用日志

`docker-compose logs -f shopsite_app`

##### 查看数据库日志

`docker-compose logs -f mysql_db`

#### 6. 访问应用

- 应用地址：http://localhost:8080
- 数据库端口：3307（映射到宿主机）

### 方式二：本地开发运行

#### 1. 启动 MySQL 数据库

#### 使用 Docker 启动 MySQL

```
docker run -d --name shopsite-mysql \
  -e MYSQL_ROOT_PASSWORD=root123 \
  -e MYSQL_DATABASE=shopsite_db \
  -e MYSQL_USER=shopuser \
  -e MYSQL_PASSWORD=shoppass123 \
  -p 3307:3306 \
  mysql:8.0
```

#### 2. 配置数据库连接

修改 `src/main/resources/application.yml`：

```yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3307/shopsite_db?useSSL=false&serverTimezone=UTC
    username: shopuser
    password: shoppass123
```

#### 3. 运行应用

##### Windows

`.\mvnw spring-boot:run -Dspring-boot.run.profiles=dev,init-data`

##### Linux/Mac

`./mvnw spring-boot:run -Dspring-boot.run.profiles=dev,init-data`

##### IDE 设置 (VS Code / IntelliJ)

- **VS Code**: 在 `launch.json` 的 `args` 中添加 `"--spring.profiles.active=dev,init-data"`。
  
- **IntelliJ**: 在 `Run Configuration` -> `Active Profiles` 中填写 `dev,init-data`。
  

> **注意**：`init-data` Profile 会在每次启动时检测数据库。如果 `category` 表为空，它将自动插入初始分类和测试商品。

#### 4. 访问应用

浏览器打开：http://localhost:8080

## 7. 测试账号

系统启动后会自动初始化以下测试账号：

| 角色  | 用户名 | 密码  | 邮箱  | 说明  |
| --- | --- | --- | --- | --- |
| **管理员** | `platformadmin` | `AdminSecurePassword123` | admin@shopsite.com | 平台管理员 |
| **管理员** | `admin` | `admin123` | admin@shopsite.com | 备用管理员 |
| **商家** | `testmerchant` | `testmerchantPASSWORD` | merchant@shopsite.com | 测试商家（已有96件商品） |
| **普通用户** | `clientuser` | `ClientSecurePassword789` | client@shopsite.com | 测试用户 |

## 8. 配置说明

### 应用配置

主要配置文件位于 `src/main/resources/`：

- **application.yml** - 主配置文件
- **application-dev.yml** - 开发环境配置
- **application-prod.yml** - 生产环境配置

### 关键配置项

#### 服务器端口

```
server:
  port: 8080
```

### 数据库配置

```
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/shopsite_db
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
```

### JPA 配置

```
  jpa:
    hibernate:
      ddl-auto: update  # 开发环境使用 update，生产环境使用 validate
    show-sql: true      # 显示 SQL 语句
```

### 文件上传配置

```
spring:
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 50MB
```

### 文件上传目录

```
file:
  upload:
    dir: uploads/
```

## 9. 安全配置

### Spring Security 配置

系统使用 Spring Security 进行认证和授权：

- **认证方式**：基于 Session 的表单登录
- **密码加密**：BCrypt 算法
- **角色权限**：
  - `ROLE_CUSTOMER` - 普通用户
  - `ROLE_MERCHANT` - 商家
  - `ROLE_ADMIN` - 管理员

### 访问控制

- 公开访问：首页、商品列表、商品详情、登录/注册页面
- 需要认证：购物车、订单、个人中心
- 商家权限：商品管理、订单处理、销售统计
- 管理员权限：商家管理、商品审核、订单监控

## 10. API 文档

### 主要 API 端点

**认证相关**

- `POST /api/auth/register` - 用户注册
- `POST /api/auth/login` - 用户登录（表单登录）

**商品相关**

- `GET /api/products` - 获取所有商品
- `GET /api/products/{id}` - 获取商品详情
- `POST /api/products` - 创建商品（商家/管理员）
- `PUT /api/products/{id}` - 更新商品（商家/管理员）
- `DELETE /api/products/{id}` - 删除商品（商家/管理员）

**订单相关**

- `POST /api/orders` - 创建订单（用户）
- `GET /api/orders` - 获取订单列表
- `GET /api/orders/{id}` - 获取订单详情

## 11. 测试

### 运行单元测试

```
.\mvnw test ### 运行特定测试类
.\mvnw test -Dtest=ProductServiceTest 
```

## 12. Docker 部署

**构建 Docker 镜像**

```
docker build -t shopsite:latest . 
```

**启动所有服务**

`docker-compose up -d`

**停止所有服务**

`docker-compose down`

**查看服务状态**

`docker-compose ps`

**查看日志**

`docker-compose logs -f`

**数据持久化**

数据库数据存储在 Docker Volume `db_data` 中，即使容器删除，数据也会保留。

## 13. 数据库设计

### 核心表结构

- **user** - 用户表（支持多角色）
- **product** - 商品表
- **category** - 分类表
- **shop_order** - 订单表
- **order_item** - 订单项表
- **cart_item** - 购物车表
- **favorite** - 收藏表
- **message** - 消息表
- **sales_log** - 销售日志表

### 数据库初始化

数据库初始化脚本位于 `src/main/resources/db/init.sql`，Docker 容器启动时会自动执行。

## 14. 开发指南

### 代码规范

- 使用 **Lombok** 简化代码（`@Data`、`@Builder` 等）
- 遵循 **RESTful API** 设计规范
- 使用 **DTO** 进行数据传输
- 使用 **@Transactional** 管理事务

### 添加新功能

1. 在 `model/` 中创建实体类
2. 在 `repository/` 中创建 Repository 接口
3. 在 `service/` 中创建 Service 接口和实现
4. 在 `controller/` 中创建 Controller
5. 在 `templates/` 中创建前端页面（如需要）

### 热部署

项目已配置 Spring Boot DevTools，修改代码后会自动重启（开发环境）。

## 15. 常见问题

### 1. 端口被占用

**Windows 查看端口占用**

`netstat -ano | findstr :8080`

**Linux/Mac 查看端口占用**

`lsof -i :8080`

### 2. 数据库连接失败

- 检查 MySQL 是否启动
- 检查数据库连接配置是否正确
- 检查防火墙设置

### 3. 文件上传失败

- 检查 `uploads/` 目录是否存在
- 检查文件大小是否超过 50MB
- 检查文件权限

### 4. Docker 容器启动失败

**查看容器日志**

`docker-compose logs shopsite_app`

**重启容器**

`docker-compose restart shopsite_app`

## 16. 许可证

本项目仅用于学习和教育目的。

## 17. 作者

华南理工大学2023级网络工程班 202330451132 刘玥

## 18. 致谢

- Spring Boot 团队
- Bootstrap 团队
- Cursor，Gemini，Deepseek，Tongyi
- 所有开源贡献者

---

**注意**：本项目为学习项目，请勿用于生产环境。生产环境部署前请进行安全加固和性能优化。