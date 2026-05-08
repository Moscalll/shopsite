# ShopSite 电商平台

基于 **Spring Boot 4**、**Java 21** 的多角色电商演示项目：普通用户购物、商家运营、平台管理员治理。页面以 **Thymeleaf 服务端渲染**为主，并辅以 `**/api/`** REST 接口**（注册、登录 JWT、商品与订单 JSON、行为上报、推荐查询等），并非独立前端 SPA 与后端完全分离的架构。

## 1. 项目简介

- **呈现方式**：浏览器访问由 Spring MVC + Thymeleaf 返回 HTML；AJAX / 移动端可调用同进程的 REST API。
- **角色**：`ROLE_CUSTOMER`（购物）、`ROLE_MERCHANT`（店铺与商品）、`ROLE_ADMIN`（平台与审计）。
- **安全**：主路径为 **Session 表单登录**（`/login`）；同时提供 `**POST /api/auth/login` 返回 JWT**，便于接口联调；密码 **BCrypt**。
- **调度**：`@EnableScheduling`，内置推荐结果定时重算任务。
- **观测**：引入 **Spring Boot Actuator**（可按需暴露健康检查等端点）。

## 2. 功能特性

### 用户端（`/`，`/product/`**，`/cart`，`/orders`，`/profile` 等）

- 注册与登录（页面表单 + `/api/auth/register`、`/api/auth/login`）
- 首页、分类列表、搜索、商品详情
- 探索 / 新品 / 热销 等导购视图
- 购物车、收藏、结算与支付页（演示流程）
- 订单列表与详情、站内消息（`/message`）
- 个性化推荐（服务端计算 + `/api/recommendations`）
- 登录用户可上报页面停留等行为（`/api/behavior/dwell`，配合前端 beacon）

### 商家端（`/merchant/**`，需 `ROLE_MERCHANT` 或 `ROLE_ADMIN`）

- 控制台、商品维护、订单处理
- 客户列表与详情、销售与统计
- 消息中心、操作与销售日志视图

### 管理员端（`/admin/**`，需 `ROLE_ADMIN`）

- 控制台、用户 / 商家 / 商品 / 订单管理
- 数据分析看板、推荐算法与结果管理
- 登录日志、用户行为日志、管理员操作日志（页面 + REST 查询）

### 横切能力

- **初始化数据**：`dev` / `init-data` 等 Profile 下可通过 `TestDataInitializer`、`CategoryInitializer`、`ProductInitializer` 等补齐演示数据。
- **日志与审计**：`AuthLoginLog`、`UserBehaviorLog`、`AdminOperationLog`、`SalesLog` 等实体及对应服务。
- **地域推断（可选）**：依赖 **ip2region**，可将 `ip2region_v4.xdb` 置于 `src/main/resources/geo/`（参见该目录下说明），用于画像等展示。

## 3. 技术栈


| 类别    | 技术                                                                              |
| ----- | ------------------------------------------------------------------------------- |
| 运行时   | Java 21                                                                         |
| 框架    | Spring Boot 4.0.x（Web MVC、Validation、Security、Data JPA、Thymeleaf、Mail、Actuator） |
| 安全    | Spring Security（表单登录 + 方法级 `@EnableMethodSecurity`）、JJWT（API 登录令牌）              |
| 持久化   | Spring Data JPA、Hibernate、MySQL 8                                               |
| 模板与样式 | Thymeleaf、Bootstrap、自定义 `static/css/custom.css`                                 |
| 工具    | Lombok、Maven                                                                    |


其他：**Tomcat** 嵌入式容器；**Docker Compose** 编排应用与 MySQL。

## 4. 架构说明

```
浏览器 ──HTTP──► Spring MVC
                    │
                    ├─► Controller（页面：返回视图名；API：JSON）
                    ├─► Service（业务 / 推荐 / 日志）
                    ├─► Repository（JPA）
                    └─► MySQL

拦截器：MessageInterceptor（全局）；OperationLogInterceptor（商家/管理员/个人资料部分路径）
安全：SecurityFilterChain 声明式授权；可选 JwtTokenProvider 用于 API 登录响应
定时任务：RecommendationRecomputeJob 周期性刷新推荐结果
```

**分层包结构（`com.example.shopsite`）**

- `**config`**：`SecurityConfig`、`WebConfig`（上传目录映射、`MessageInterceptor`）、`WebMvcConfig`（后台操作日志拦截）、各类 `***Initializer**` 测试数据 / 分类 / 商品初始化。
- `**controller**`：根级 REST（如 `AuthController`、`ProductController`、`OrderController`）；子包 `**user**` / `**merchant**` / `**admin**`（页面流）；`**api**`（如行为上报 `BehaviorDwellRestController`）。
- `**service**` / `**service.impl**`：业务实现。
- `**repository**`：JPA 接口。
- `**model**`：领域实体（含订单、购物车、收藏、消息、各类日志、推荐结果等）。
- `**dto**`：请求 / 传输对象。
- `**security**`：认证成功/失败处理、JWT 组件等。
- `**interceptor**`：消息未读数等通用拦截。
- `**job**`：定时推荐重算。
- `**support**`：客户端 IP、类目文案等辅助逻辑。
- `**exception**` / `**handler**`：业务异常与全局处理。

**模板目录（`src/main/resources/templates`）**

- `**layout`**、`index`：站点壳层与首页片段。
- `**auth**`：登录 / 注册。
- `**user**`、`merchant`**、`admin`**：各角色页面。
- `**product**`：部分列表视图。

静态资源在 `**static/**`；上传文件通过配置项映射到 `**/uploads/****`（默认目录 `uploads/`）。

## 5. 项目结构（精简）

```
shopsite/
├── src/main/java/com/example/shopsite/
│   ├── config/           # 安全、Web、初始化器
│   ├── controller/       # 页面控制器 + REST（含 admin / merchant / user / api）
│   ├── dto/
│   ├── exception/
│   ├── handler/
│   ├── interceptor/
│   ├── job/              # 定时任务（推荐）
│   ├── model/
│   ├── repository/
│   ├── security/
│   ├── service/ + impl/
│   ├── support/
│   └── ShopsiteApplication.java
├── src/main/resources/
│   ├── db/init.sql       # Docker 首次初始化挂载脚本（与 JPA 协同，保持幂等）
│   ├── templates/
│   ├── static/
│   └── geo/              # 可选 ip2region 数据文件说明
├── uploads/              # 本地上传目录（运行时可配置）
├── docker-compose.yml
├── Dockerfile
├── pom.xml
└── README.md
```

在仓库中若未看到 `application.yml`，请在本地 `**src/main/resources/**` 自行添加主配置或 `**application-dev.yml` / `application-prod.yml**`，并通过环境变量或 Profile 注入数据源等（与 Docker Compose 中的 `SPRING_PROFILES_ACTIVE`、`PROD_DB_*` 等对应）。

## 6. 环境要求

- **JDK** 21+
- **Maven** 3.6+（或使用 `mvnw`）
- **MySQL** 8.0
- **Docker** / **Docker Compose**（可选，用于一键启动）

## 7. 快速开始

### 方式一：Docker Compose

1. 克隆仓库并进入项目根目录。
2. 创建 `.env`（示例与 README 历史版本一致），包含 MySQL 与应用库账号、`PROD_DB_URL` 等。
3. 构建：`mvnw clean package -DskipTests`（Windows 使用 `.\mvnw`）。
4. 启动：`docker-compose up -d`
  - Compose 中应用使用 `**SPRING_PROFILES_ACTIVE=prod,init-data`**，数据库挂载 `**src/main/resources/db**` 到初始化目录。
5. 访问：**[http://localhost:8080](http://localhost:8080)**，MySQL 映射端口 **3307**。

### 方式二：本地运行

1. 启动 MySQL（可用 Docker 映射 `3307:3306`）。
2. 配置 `**spring.datasource.*`** 指向你的实例（示例端口可与上文一致）。
3. 运行：

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev,init-data
```

Windows：`.\mvnw spring-boot:run "-Dspring-boot.run.profiles=dev,init-data"`

IDE 中在 **Active profiles** 填入 `**dev,init-data`**。`init-data` 会在数据为空时写入演示分类与商品等（逻辑见 `TestDataInitializer` 等）。

## 8. 测试账号

启动并初始化后，可使用（具体以 `TestDataInitializer` / 数据库为准）：


| 角色  | 用户名             | 密码                        | 说明    |
| --- | --------------- | ------------------------- | ----- |
| 管理员 | `platformadmin` | `AdminSecurePassword123`  | 平台管理员 |
| 管理员 | `admin`         | `admin123`                | 备用    |
| 商家  | `testmerchant`  | `testmerchantPASSWORD`    | 演示店铺  |
| 用户  | `clientuser`    | `ClientSecurePassword789` | 演示买家  |


## 9. 配置与安全要点

- **端口**：默认 **8080**（可通过配置修改）。
- **上传**：应用启动类中调整了 Tomcat 表单 POST 与参数数量上限；Spring 侧可配合 `multipart` 大小限制。
- **CSRF**：对 `**/api/auth/register`**、`**/api/behavior/dwell**` 忽略 CSRF，其余仍遵循表单 / 会话策略。
- **路径授权摘要**（详见 `SecurityConfig`）：匿名可访问首页、商品浏览、`/login`、`/register`、静态资源、部分 GET `**/api/products/**`**；`**/merchant/****` 需商家或管理员；`**/admin/****` 需管理员；下单等接口绑定 `**ROLE_CUSTOMER**`。

## 10. 主要 HTTP 接口（节选）

以下为常见的 REST 前缀；完整列表以各 Controller 注解为准。


| 前缀                                      | 说明                  |
| --------------------------------------- | ------------------- |
| `POST /api/auth/register`               | 注册                  |
| `POST /api/auth/login`                  | 登录，响应中含 JWT         |
| `GET/POST/PUT/DELETE /api/products/**`  | 商品 CRUD（写操作需商家或管理员） |
| `GET /api/categories`                   | 分类                  |
| `POST /api/orders`、`GET /api/orders/**` | 订单（创建对客户角色有约束）      |
| `GET /api/recommendations`              | 推荐                  |
| `POST /api/behavior/dwell`              | 停留等行为上报（需登录）        |
| `GET /api/admin/orders/{id}`            | 管理员订单 JSON          |
| `GET/POST /api/admin/logs/**`           | 日志查询 API            |


页面型路由示例：`**/cart/****`、`**/orders/****`、`**/merchant/****`、`**/admin/****`、`**/profile/****`、`**/checkout**`、`**/payment**`。

## 11. 数据库与领域模型

表结构主要由 **JPA** 维护；`db/init.sql` 用于 Docker 首次建库时的安全补充。核心实体包括：

`User`、`Product`、`Category`、`Order` / `OrderItem`、`CartItem`、`Favorite`、`Message`；以及 `**UserBehaviorLog`**、`**AuthLoginLog**`、`**AdminOperationLog**`、`**SalesLog**`、`**RecommendationResult**` 等。

## 12. 测试与构建

```bash
./mvnw test
./mvnw test -Dtest=YourTestClass
```

项目可选 **Spring Boot DevTools**，开发模式下保存变更可触发热重启。

## 13. Docker 镜像（可选）

```bash
docker build -t shopsite:latest .
docker-compose up -d
docker-compose down
```

数据卷 `**db_data**` 持久化 MySQL 数据。

## 14. 常见问题

- **端口占用**：Windows `netstat -ano | findstr :8080`；Linux/macOS `lsof -i :8080`。
- **数据库连不上**：检查 MySQL 是否监听、账号密码、URL 中的主机名（Docker 网络内用服务名）。
- **上传失败**：确认 `**file.upload.dir`** 对应目录存在且进程可写。

## 15. 许可证

本项目仅用于学习和教育目的。

## 16. 作者

华南理工大学2023级网络工程班 202330451132 刘玥

## 17. 致谢

- Spring Boot 团队  
- Bootstrap 团队  
- Cursor，Gemini，Deepseek，Tongyi  
- 所有开源贡献者

---

**注意**：本项目为学习演示用途；若用于真实生产，请进行安全加固、审计与性能评估。