# ShopSite 电商平台

一个基于 Spring Boot 4.0 开发的全功能电商平台，支持多角色管理（用户、商家、管理员），提供完整的购物流程、订单管理和商家后台功能。

**亮点速览**：除多角色电商业务与 **Thymeleaf + React（Vite）+ JWT API** 前后端能力外，仓库自带 **`tools/` 批量商品上新自动化工具链**——从 MySQL 读取分类与已有商品、由 **LLM（推荐 Ollama）** 生成新品规格与主图 **prompt / catalog**、通过 **ComfyUI**（HTTP API 工作流）或 **Pollinations** 等批量出图，再 **`INSERT` 写入数据库**并将图片同步至 **`uploads/`**；支持 PowerShell 一键脚本 **`batch_new_products.ps1`**（控制条数、可跳过出图/导入、同名跳过防重复）。完整说明见下文 **[第 15 节：批量商品上新自动化工具链](#readme-tools)** 与 [`tools/README.md`](tools/README.md)。

## 目录

- [1. 项目简介](#1-项目简介)
- [2. 功能特性](#2-功能特性)
- [3. 技术栈](#3-技术栈)
- [4. 项目结构](#4-项目结构)
- [5. 环境要求](#5-环境要求)
- [6. 快速开始](#6-快速开始)
- [7. 测试账号](#7-测试账号)
- [8. 配置说明](#8-配置说明)
- [9. 安全配置](#9-安全配置)
- [10. API 文档](#10-api-文档)
- [11. 测试](#11-测试)
- [12. Docker 部署](#12-docker-部署)
- [13. 数据库设计](#13-数据库设计)
- [14. 开发指南](#14-开发指南)
- [15. 批量商品上新自动化工具链（tools/）](#readme-tools)
- [16. 常见问题](#16-常见问题)
- [17. 许可证](#17-许可证)
- [18. 作者](#18-作者)
- [19. 致谢](#19-致谢)

## 1. 项目简介

ShopSite 是一个现代化的电商平台系统：**后端**使用 Spring Boot 提供 RESTful API 与传统 MVC 页面；**前端**同时保留 **Thymeleaf** 服务端渲染页面，并在 `frontend/` 目录提供基于 **Vite + React** 的单页应用（SPA），用于登录、商品浏览与商家商品管理等场景，通过 **JWT（Bearer Token）** 调用 `/api` 接口。系统支持三种角色：普通用户（购物）、商家（商品管理）、管理员（平台管理）。

<img width="1280" height="730" alt="chrome_HYlAGvybQV" src="https://github.com/user-attachments/assets/7b60621c-a22c-4f74-9e34-e5a6acb170c1" />

## 2. 功能特性

### 用户端功能

- 用户注册与登录（Spring Security；Web 表单登录 + API 的 JWT 登录）
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

- **Thymeleaf** - 服务端模板引擎（传统页面与表单登录）
- **Bootstrap** 5.x - CSS 框架（与模板页配套）
- **React** 19 + **TypeScript** + **Vite** - `frontend/` SPA 开发与构建
- **Ant Design** - SPA UI 组件库
- **Axios** - HTTP 客户端（请求拦截器附加 `Authorization: Bearer <token>`）
- **Zustand** - 前端状态管理
- **React Router** - SPA 路由

### 开发工具

- **Maven** - 项目构建工具
- **Lombok** - 代码简化
- **Docker** - 容器化部署
- **Docker Compose** - 多容器编排

### 其他

- **Java** 21 (LTS)
- **JWT（jjwt）** - SPA / JSON API 请求的身份认证（`Authorization: Bearer`）
- **BCrypt** - 密码加密

需在配置中提供 JWT 相关属性（例如环境变量或 YAML）：`jwt.secret`（Base64 编码的密钥）、`jwt.expiration`（毫秒，过期时间）。

## 4. 项目结构

```
shopsite-v2/
├── frontend/                 # React SPA（Vite）
│   ├── src/                  # 页面、路由、状态、HTTP 封装等
│   ├── package.json
│   └── vite.config.ts        # 开发代理：将 /api 转发到后端（默认 http://localhost:8080）
├── tools/                    # 批量商品上新自动化工具链（Python，详见本章与 tools/README.md）
├── src/
│ ├── main/
│ │ ├── java/com/example/shopsite/
│ │ │ ├── config/             # 配置类（安全、初始化等）
│ │ │ ├── controller/         # 控制器层（含 REST `/api/*`）
│ │ │ ├── dto/                # 数据传输对象
│ │ │ ├── exception/          # 异常类
│ │ │ ├── handler/            # 异常处理器
│ │ │ ├── interceptor/        # 拦截器
│ │ │ ├── model/              # 实体类（User, Product, Order 等）
│ │ │ ├── repository/         # 数据访问层
│ │ │ ├── security/           # JWT 过滤器、Token 提供者等
│ │ │ ├── service/            # 业务逻辑层
│ │ │ │ └── impl/             # 服务实现类
│ │ │ └── ShopsiteApplication.java
│ │ └── resources/
│ │ ├── application.yml       # 主配置文件
│ │ ├── application-dev.yml
│ │ ├── application-prod.yml
│ │ ├── db/
│ │ │ └── init.sql            # 数据库初始化脚本
│ │ ├── static/               # 静态资源（CSS、JS、图片）
│ │ └── templates/            # Thymeleaf 模板
│ └── test/                   # 测试代码
├── uploads/                  # 文件上传目录
├── docker-compose.yml
├── Dockerfile
├── pom.xml
└── README.md
```

## 5. 环境要求

### 必需环境

- **JDK** 21 或更高版本
- **Maven** 3.6+（或使用项目自带的 Maven Wrapper）
- **MySQL** 8.0
- **Docker** 和 **Docker Compose**（推荐用于快速部署）

### 可选工具

- **Node.js** LTS + **npm**（构建与调试 `frontend/` SPA）
- **IntelliJ IDEA** / **Eclipse** / **VS Code**（IDE）
- **Postman** / **Thunder Client**（API 测试）
- **MySQL Workbench** / **Navicat**（数据库管理）

## 6. 快速开始

### 方式一：Docker Compose 部署（推荐）

#### 1. 克隆项目

`git clone <项目地址>
cd shopsite-v2`

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

**使用 Docker 启动 MySQL**

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

日常开发可直接使用默认 profile（根目录 `application.yml` 已激活 `dev`），或显式指定：

##### Windows

`.\mvnw spring-boot:run -Dspring-boot.run.profiles=dev`

##### Linux/Mac

`./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`

##### IDE 设置 (VS Code / IntelliJ)

- **VS Code**: 在 `launch.json` 的 `args` 中添加 `"--spring.profiles.active=dev"`（按需追加 `,init-data`）。
- **IntelliJ**: 在 `Run Configuration` -> `Active Profiles` 中填写 `dev`（按需追加 `init-data`）。

> **注意**：**不要**在日常开发中频繁启用 `init-data`：该 profile 会重置测试数据（包括登录日志等）。仅在需要一键重置演示数据时，在启动参数中显式使用 `dev,init-data`。

#### 4. （可选）启动 React 前端开发服务

在另一个终端进入 `frontend/` 目录，安装依赖并启动 Vite（默认端口一般为 `5173`，`/api` 会代理到后端 `8080`）：

```
cd frontend
npm install
npm run dev
```

可通过环境变量配置代理目标与 API 根路径，例如：

- `frontend/.env.development` 中设置 `VITE_API_PROXY_TARGET=http://127.0.0.1:8080`（后端地址）
- 若希望 Axios 直接请求完整后端 URL，可设置 `VITE_API_BASE_URL`（与代理二选一或按需组合）

#### 5. 访问应用

- **后端（Thymeleaf / 同一端口上的 API）**：http://localhost:8080  
- **仅开发 SPA 时**：一般以 Vite 提示的本地地址为准（如 http://localhost:5173），接口经代理访问后端

## 7. 测试账号

在完成数据库初始化（例如使用 `init-data` profile 或执行 `db/init.sql` 等流程）后，可使用下列内置测试账号：

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

#### JWT（启动前需配置）

应用中的 `JwtTokenProvider` 使用以下配置项（请放在可被 Spring 加载的 YAML 或环境变量中）：

```
jwt:
  secret: <Base64 编码的密钥>
  expiration: 86400000   # 示例：毫秒，如 24 小时
```

#### 服务器端口

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

系统使用 Spring Security 进行认证和授权，并采用 **双通道** 设计：

- **浏览器 / Thymeleaf**：基于 **Session** 的表单登录（`/login`），登录页等仍受 **CSRF** 保护。
- **SPA 与 JSON API**：`POST /api/auth/login` 校验用户名密码后返回 **JWT**；客户端在后续请求头中携带 `Authorization: Bearer <token>`。`JwtAuthenticationFilter` 在表单认证之前解析 JWT 并建立安全上下文。`/api/**` 路径在 CSRF 上单独放宽，以便无 Session 的 API 调用。

- **密码加密**：BCrypt 算法  
- **角色权限**：
  - `ROLE_CUSTOMER` - 普通用户
  - `ROLE_MERCHANT` - 商家
  - `ROLE_ADMIN` - 管理员

### 访问控制（与代码一致的主要规则）

- **匿名可访问**：静态资源、部分页面路径、`POST /api/auth/register`、`POST /api/auth/login`，以及 **GET** `/api/categories/**`、**GET** `/api/recommendations/**`、**GET** `/api/products/**`（含分页列表）等。
- **商家/管理员**：**GET** `/api/products/mine`（当前商家的商品分页）。
- **商家或管理员**：**POST/PUT/DELETE** `/api/products` 及子路径（创建/修改/删除商品）。
- **普通用户**：**POST** `/api/orders`（下单）。
- **已登录用户**：购物车、收藏、订单页面路径、`/api/orders/**` 等；`/merchant/**`、`/admin/**` 按角色限制。

具体以 `SecurityConfig` 中 `authorizeHttpRequests` 为准。

## 10. API 文档

### 主要 API 端点

**认证相关**

- `POST /api/auth/register` - 用户注册（JSON）
- `POST /api/auth/login` - **JSON 登录**，成功返回 `accessToken`、`tokenType`（如 `Bearer`）、`expiresIn`（秒），供 SPA 存储并在后续请求头中使用 `Authorization: Bearer <accessToken>`

**用户相关**

- `GET /api/users/me` - 当前登录用户信息（需认证；JWT 或 Session 均可）

**商品相关**

- `GET /api/products` - 在售商品分页列表（默认按 `id` 降序，每页 12 条，支持 Spring Data 分页参数）
- `GET /api/products/mine` - 当前登录 **商家/管理员** 的商品分页（默认每页 10 条）
- `POST /api/products?categoryId=` - 创建商品（商家/管理员；请求体为商品 JSON）
- `PUT /api/products/{id}` - 更新商品（商家/管理员）
- `DELETE /api/products/{id}` - 删除商品（商家/管理员）

**订单相关**

- `POST /api/orders` - 创建订单（用户）
- `GET /api/orders` - 获取订单列表
- `GET /api/orders/{id}` - 获取订单详情

更多端点以各 `Controller` 与 `SecurityConfig` 中的放行规则为准。

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
4. 在 `controller/` 中创建 Controller（REST 需在 `SecurityConfig` 中配置访问规则）
5. 在 `templates/` 中创建 Thymeleaf 页面，或在 `frontend/src/` 中开发 SPA 页面并接入 `/api`

### 热部署

项目已配置 Spring Boot DevTools，修改代码后会自动重启（开发环境）。

<a id="readme-tools"></a>

## 15. 批量商品上新自动化工具链（`tools/`）

仓库下的 **`tools/`** 目录提供与主应用 **解耦** 的 Python 工具链：批量生成商品规格与主图提示词、（可选）通过 **ComfyUI HTTP API** 或 **Pollinations** 等批量出图，再使用 **`INSERT`** 写入 MySQL，并把图片拷贝到站点运行时目录（默认 **`uploads/`**），用于 **自动化上新**。该工具链 **不修改** Java / 前端源码。

### 流水线在做什么

1. **（可选）读取数据库**：`pipeline/fetch_existing.py` 拉取分类与该商户已有商品信息，供 LLM 生成「不撞车」的新品提案。
2. **生成商品数据**：手工编写 `product_spec.jsonl`，或由 **`generate_candidates_llm.py`**（需配置 LLM，**Windows 上推荐 Ollama HTTP**，见 `pipeline/config.example.yaml`）自动生成 `product_spec.auto.jsonl`。
3. **生成提示词与目录**：`product_pipeline/generate.py` 产出 **`prompts.jsonl`**、**`catalog.jsonl`**（主图文件名与导入字段对齐）。
4. **出图**：`image_render/render_batch.py` 按提示词批量保存图片；一键脚本里可用 `-SkipRender` 跳过。
5. **入库**：`db_import/import_products.py` 写入数据库并拷贝图片；建议上新时使用 **`--skip-if-name-exists`**，避免同一商户下 **商品名重复**。

一键脚本 **`pipeline/batch_new_products.ps1`** 会串联上述步骤（默认含导入，除非 `-SkipImport`）；**`pipeline/run_all.ps1`** 侧重生成链路，若设置环境变量 **`SHOPSITE_AUTO_RENDER=1`** 且已配置 **`image_render/config.yaml`**，可在生成 `prompts.jsonl` 后继续自动出图。

### 快速接入

```powershell
cd tools
python -m venv .venv
.\.venv\Scripts\activate   # Linux/macOS: source .venv/bin/activate
pip install -r requirements.txt
copy .env.example .env   # 填写 MYSQL_*、SHOPSITE_UPLOAD_DIR 等；Unix: cp .env.example .env
copy pipeline\config.example.yaml pipeline\config.yaml   # Unix: cp pipeline/config.example.yaml pipeline/config.yaml
.\pipeline\batch_new_products.ps1 -MaxProducts 8
```

**说明**：`batch_new_products.ps1` 为 **Windows PowerShell** 脚本；在 Linux / macOS 上请按 [`tools/pipeline/README.md`](tools/pipeline/README.md) 中的等价命令 **分步执行** Python 脚本。

参数说明（如 `-MaxProducts`、`-SkipRender`、`-SkipImport`、`-NonInteractive`）与 **Ollama / ComfyUI / 手动 Colab** 等细节，以 **[`tools/README.md`](tools/README.md)**、**[`tools/pipeline/README.md`](tools/pipeline/README.md)**、`tools/image_render/README.md`、`tools/comfyui/README.md` 为准。

### 上架可见条件

导入脚本默认 **`is_available = true`**；站点前台展示可售商品通常还要求 **`stock >= 1`**（与 `ProductRepository` 等查询一致），请在规格或导入逻辑中保证库存字段合理。

## 16. 常见问题

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

## 17. 许可证

本项目仅用于学习和教育目的。

## 18. 作者

华南理工大学2023级网络工程班 202330451132 刘玥

## 19. 致谢

### 应用与基础设施

- **Spring Boot** 及 Spring 生态维护者
- **React**、**Vite**、**Ant Design** 等前端开源项目
- **Bootstrap** 团队

### 自动化上新与生成式工具链（`tools/`）

- **[ComfyUI](https://github.com/comfyanonymous/ComfyUI)** 与工作流社区——本仓库通过 HTTP API 批量出主图
- **[Ollama](https://ollama.com)**——本地 LLM 推理，用于流水线中的新品文案/规格生成（推荐部署方式）
- **[Pollinations](https://pollinations.ai)**——可选云端图像生成接口（见 `tools/image_render`）
- **Python** 生态（**PyTorch** 等与 ComfyUI / 本地推理相关的上游项目）

### 开发与协作

- **Cursor**，**Gemini**，**Deepseek**，**通义（Tongyi）** 等在开发与文档整理中的协助
- 所有为本项目依赖做出贡献的**开源作者与社区**

---

**注意**：本项目为学习项目，请勿用于生产环境。生产环境部署前请进行安全加固和性能优化。
