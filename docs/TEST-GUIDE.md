# ShopSite 测试方法说明

本文档描述 ShopSite 电商演示项目的测试策略、用例清单与一键执行方式。  
自动化脚本位于 `scripts/functional/` 与 `scripts/run-all-tests.ps1`；首屏性能测试沿用 `scripts/perf/`。

---

## 1. 测试目标与范围

| 类型 | 说明 | 自动化 |
|------|------|--------|
| **黑盒测试** | 不关注内部实现，从用户/调用方视角验证输入输出与页面行为 | API 冒烟 + E2E |
| **功能测试** | 按角色验证业务流程（浏览、登录、购物流、后台管理） | E2E + 部分 API |
| **性能测试** | 首屏 FCP/LCP/CLS、各角色关键页加载 | `npm run perf:*` |
| **单元测试** | Service/Controller 层逻辑（Mockito/MockMvc） | `mvnw test` |

**不在本仓库自动化范围内（需人工或后续补充）：** 邮件发送、支付网关真实扣款、高并发压测、安全渗透。

---

## 2. 测试环境与前置条件

### 2.1 环境

- **JDK 21+**、**Maven**（或 `mvnw`）
- **Node.js 18+**（Playwright 功能/性能脚本）
- **MySQL 8** 已启动，应用 profile 为 `dev`（或 `dev,init-data` 重置演示数据）
- 应用在 **http://localhost:8080** 运行

### 2.2 四个标准测试账号

由 `TestDataInitializer`（`init-data` profile）写入，与 README 一致：

| 角色 | 用户名 | 密码 | 典型用途 |
|------|--------|------|----------|
| 平台管理员 | `platformadmin` | `AdminSecurePassword123` | `/admin/**`、审计日志 |
| 备用管理员 | `admin` | `admin123` | 管理员登录回归（**init-data 默认不创建**，需手工注册或自行插入） |
| 商家 | `testmerchant` | `testmerchantPASSWORD` | `/merchant/**`、商品维护 |
| 买家 | `clientuser` | `ClientSecurePassword789` | 购物车、下单、收藏 |

可通过环境变量覆盖脚本中的账号（见第 6 节）。

### 2.3 启动被测应用

```powershell
cd D:\GitHub\shopsite
$env:DB_USERNAME = "shopsite_user"
$env:DB_PASSWORD = "761943"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

> **注意：** `init-data` 每次启动会**清空并重建**数据库。日常联调只用 `dev`；需要重置四个账号与演示商品时再临时加 `init-data`。

---

## 3. 黑盒测试

黑盒测试只关心「给定输入是否得到预期外部行为」，不验证 JPA/SQL 实现细节。

### 3.1 公开页面（匿名）

| 编号 | 用例 | 预期 |
|------|------|------|
| BB-P01 | GET `/` | 200，首页可渲染 |
| BB-P02 | GET `/products` | 200，列表或「暂无商品」 |
| BB-P03 | GET `/product/{id}` | 200，详情字段可见 |
| BB-P04 | GET `/explore`、`/new-arrivals`、`/top-selling` | 200，导购页可访问 |
| BB-P05 | GET `/help`、`/about` | 200 |
| BB-P06 | GET `/login`、`/register` | 200，表单存在 |
| BB-P07 | GET `/search?q=关键词`（**需登录**） | 200 |

**自动化：** `run-functional-tests.mjs` → 分类 `blackbox-public`、`functional-browse`

### 3.2 公开 REST API

| 编号 | 用例 | 预期 |
|------|------|------|
| BB-A01 | GET `/api/products` | 200，JSON 数组，元素含 id/name/price/stock |
| BB-A02 | GET `/api/categories` | 200，至少 1 个分类 |
| BB-A03 | POST `/api/auth/login` 正确凭据 | 200，响应含 `Token:` |
| BB-A04 | POST `/api/auth/login` 错误密码 | 401 |
| BB-A05 | POST `/api/auth/register` 新用户 | 201 |
| BB-A06 | POST `/api/auth/register` 重复用户名 | 400 |

**自动化：** `run-api-smoke-tests.mjs` → 分类 `public-api`、`auth-api`

### 3.3 安全黑盒（未授权 / 越权）

| 编号 | 用例 | 预期 |
|------|------|------|
| BB-S01 | 未登录 GET `/cart` | 跳转 `/login` |
| BB-S02 | 未登录 GET `/admin` | 跳转 `/login` |
| BB-S03 | 未登录 GET `/api/orders/my` | 401 / 403 / 302 |
| BB-S04 | 客户 JWT GET `/api/admin/orders` | 403 |
| BB-S05 | 客户访问 `/admin` | 拒绝（登录页或错误页） |
| BB-S06 | 商家访问 `/admin` | 拒绝 |

**自动化：** API → `security-api`、`role-api`；UI → `security-ui`

---

## 4. 功能测试（按角色）

### 4.1 买家（clientuser）

| 编号 | 场景 | 步骤 | 预期 |
|------|------|------|------|
| FN-C01 | 登录 | `/login` 填账号密码 | 进入首页，导航可见 |
| FN-C02 | 浏览订单/收藏/资料 | 访问 `/orders`、`/favorites`、`/profile` | 不跳转登录 |
| FN-C03 | 加购 | 商品详情 →「加入购物车」→ `/cart` | 购物车非空 |
| FN-C04 | 推荐 | GET `/api/recommendations/my`（JWT） | 200，返回 ID 数组 |
| FN-C05 | 下单（可选） | POST `/api/orders` | 201/200，库存扣减 |

**自动化：** E2E `functional-customer`；下单需 `ENABLE_ORDER_TEST=1`

### 4.2 商家（testmerchant）

| 编号 | 场景 | 步骤 | 预期 |
|------|------|------|------|
| FN-M01 | 登录 | 表单登录 | 成功 |
| FN-M02 | 控制台 | `/merchant`、`/merchant/orders` 等 | 200，非登录页 |
| FN-M03 | 订单 API | GET `/api/admin/orders`（JWT） | 200 |
| FN-M04 | 创建商品 | POST `/api/products?categoryId=1`（JWT） | 2xx 或业务 4xx（参数校验） |

**自动化：** E2E `functional-merchant`；API `role-api`

### 4.3 管理员（platformadmin / admin）

| 编号 | 场景 | 步骤 | 预期 |
|------|------|------|------|
| FN-A01 | 登录 | 两个管理员账号均可登录 | 成功 |
| FN-A02 | 后台页面 | `/admin`、`/admin/users`、`/admin/orders` 等 | 可访问 |
| FN-A03 | 订单 API | GET `/api/admin/orders`（JWT） | 200 |

**自动化：** E2E `functional-admin`

### 4.4 人工回归清单（尚未全自动）

以下建议发版前人工走查：

1. **结算全流程：** 购物车勾选 → `/checkout` → `/payment` → 支付成功页  
2. **商家发货：** 买家下单并支付 → 商家 `/merchant/orders/{id}` 发货 → 状态变更  
3. **管理员审计：** `/admin/logs/login`、`/admin/logs/behavior` 是否有新记录  
4. **商品上传：** 商家上传图片，确认 `/uploads/**` 可访问  
5. **注册页面：** `/register` 表单注册新 CUSTOMER  

---

## 5. 性能测试（已有）

首屏与登录后关键页性能见 [`perf/perf-test-guide.md`](../perf/perf-test-guide.md)。

| 脚本 | 说明 |
|------|------|
| `npm run perf:collect` | 匿名页 FCP/LCP/CLS（默认 `/`, `/products`, `/product/1`） |
| `npm run perf:auth-collect` | 三角色登录后业务页 |
| `npm run perf:compare` | 对比两次 JSON 结果 |
| `npm run perf:report` | 生成可读报告 |

环境变量：`BASE_URL`、`RUNS`、`TARGETS`、`VERSION`、`OUT_DIR`（默认 `perf-results/`）。

---

## 6. 一键测试

### 6.1 推荐命令（PowerShell）

```powershell
cd D:\GitHub\shopsite

# 安装 Node 依赖（首次）
npm install
npx playwright install chromium

# 一键：Maven + API 黑盒 + 页面功能
.\scripts\run-all-tests.ps1

# 含首屏性能
.\scripts\run-all-tests.ps1 -IncludePerf

# 仅 API + 功能（跳过 Maven）
.\scripts\run-all-tests.ps1 -SkipMaven

# 指定地址 / 启用下单 API 测试（会消耗库存）
.\scripts\run-all-tests.ps1 -BaseUrl "http://localhost:8080" -EnableOrderTest
```

### 6.2 npm 快捷命令

```powershell
npm run test:api          # API 黑盒
npm run test:functional   # 页面功能 E2E
npm run test:all          # 调用 run-all-tests.ps1（SkipMaven）
```

### 6.3 报告输出

| 目录 | 内容 |
|------|------|
| `test-results/api-smoke_*.json` | API 用例逐条 pass/fail |
| `test-results/functional-e2e_*.json` | E2E 用例逐条 pass/fail |
| `perf-results/*.json` | 性能指标（`-IncludePerf` 时） |

### 6.4 环境变量（账号与地址）

```powershell
$env:BASE_URL = "http://localhost:8080"
$env:ADMIN_USER = "platformadmin"
$env:ADMIN_PASS = "AdminSecurePassword123"
$env:MERCHANT_USER = "testmerchant"
$env:MERCHANT_PASS = "testmerchantPASSWORD"
$env:CUSTOMER_USER = "clientuser"
$env:CUSTOMER_PASS = "ClientSecurePassword789"
$env:ENABLE_ORDER_TEST = "1"   # 可选：API 创建订单
```

---

## 7. Maven 单元测试

```powershell
.\mvnw.cmd test
.\mvnw.cmd test -Dtest=OrderServiceImplTest
```

仓库内部分测试类目前为注释状态（`ShopsiteApplicationTests`、`OrderServiceImplTest` 等）。启用后纳入 `run-all-tests.ps1` 默认流程；开发阶段可用 `-SkipMaven` 仅跑黑盒/功能脚本。

**建议补充的单元测试方向：**

- `OrderServiceImpl`：库存不足、总价计算、状态机  
- `SecurityConfig` / REST：MockMvc 角色 401/403  
- `RecommendationService`：空用户、topN 边界  

---

## 8. 通过标准（CI 建议）

| 阶段 | 通过条件 |
|------|----------|
| API 冒烟 | 全部 `passed`，exit code 0 |
| 功能 E2E | 全部 `passed`，exit code 0 |
| Maven | `BUILD SUCCESS` |
| 性能（可选） | LCP P50 不高于基线 +20%；无新增 5xx |

---

## 9. 故障排查

| 现象 | 处理 |
|------|------|
| `站点未响应` | 先启动 Spring Boot，确认 8080 端口 |
| Playwright 浏览器缺失 | `npx playwright install chromium` |
| 登录失败 | 确认 DB 有测试账号；或 `dev,init-data` 重置 |
| API 403 但预期 200 | 检查 JWT 是否携带 `Authorization: Bearer` |
| 加购后购物车仍空 | 商品 `stock=0` 或 `is_available=false`；换有库存商品 |
| Maven 测试失败 | 使用 `-SkipMaven`，或取消注释并修复单元测试 |

---

## 10. 文件索引

```
scripts/
├── run-all-tests.ps1              # 一键入口
├── functional/
│   ├── config.mjs                 # 账号、页面清单
│   ├── lib.mjs                    # 报告、HTTP 工具
│   ├── run-api-smoke-tests.mjs    # API 黑盒
│   └── run-functional-tests.mjs   # 页面功能 E2E
└── perf/
    ├── collect-perf-metrics.mjs   # 匿名首屏性能（已有）
    └── collect-auth-perf-metrics.mjs  # 登录后性能（已有）
docs/
└── TEST-GUIDE.md                  # 本文档
```
