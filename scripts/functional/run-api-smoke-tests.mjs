/**
 * ShopSite REST API 黑盒冒烟测试
 * 覆盖：公开接口、认证、角色授权、基础业务 JSON 接口
 */
import { ACCOUNTS } from './config.mjs';
import {
  TestReporter,
  apiGet,
  apiPost,
  assert,
  assertStatus,
  isAccessDenied,
  loginWithJwt,
  loginWithSession,
  waitForServer,
} from './lib.mjs';

async function main() {
  console.log('\n== API 黑盒冒烟测试 ==');
  await waitForServer();

  const reporter = new TestReporter('api-smoke');
  let products = [];
  let categories = [];

  // ── 公开读接口 ──
  await reporter.run('GET /api/products 返回可售商品列表', 'public-api', async () => {
    const { status, body } = await apiGet('/api/products');
    assertStatus(status, 200, '商品列表接口');
    assert(Array.isArray(body), '响应应为数组');
    products = body;
    return { count: body.length };
  });

  await reporter.run('GET /api/categories 返回分类列表', 'public-api', async () => {
    const { status, body } = await apiGet('/api/categories');
    assertStatus(status, 200, '分类接口');
    assert(Array.isArray(body), '响应应为数组');
    assert(body.length >= 1, '至少应有一个分类');
    categories = body;
    return { count: body.length };
  });

  await reporter.run('GET /api/products 商品字段完整性', 'public-api', async () => {
    if (!products.length) return { skipped: '无商品，跳过字段校验' };
    const p = products[0];
    assert(p.id != null, '商品应有 id');
    assert(typeof p.name === 'string' && p.name.length > 0, '商品应有 name');
    assert(p.price != null, '商品应有 price');
    assert(typeof p.stock === 'number', '商品应有 stock');
    return { sampleId: p.id, sampleName: p.name };
  });

  // ── 认证接口 ──
  await reporter.run('POST /api/auth/login 正确凭据返回 Token', 'auth-api', async () => {
    const { status, body } = await apiPost('/api/auth/login', {
      username: ACCOUNTS.customer.username,
      password: ACCOUNTS.customer.password,
    });
    assertStatus(status, 200, '客户登录');
    assert(String(body).includes('Token:'), '响应应包含 JWT Token');
    return { username: ACCOUNTS.customer.username };
  });

  await reporter.run('POST /api/auth/login 错误密码返回 401', 'auth-api', async () => {
    const { status } = await apiPost('/api/auth/login', {
      username: ACCOUNTS.customer.username,
      password: 'WrongPassword!!!',
    });
    assertStatus(status, 401, '错误密码');
  });

  await reporter.run('POST /api/auth/register 新用户注册成功', 'auth-api', async () => {
    const suffix = Date.now();
    const { status, body } = await apiPost('/api/auth/register', {
      username: `auto_test_${suffix}`,
      email: `auto_test_${suffix}@example.com`,
      password: 'AutoTestPass123!',
    });
    assertStatus(status, 201, '注册');
    assert(String(body).includes('注册成功'), '应返回注册成功消息');
    return { username: `auto_test_${suffix}` };
  });

  await reporter.run('POST /api/auth/register 重复用户名返回 400', 'auth-api', async () => {
    const { status } = await apiPost('/api/auth/register', {
      username: ACCOUNTS.customer.username,
      email: 'duplicate@example.com',
      password: 'AutoTestPass123!',
    });
    assertStatus(status, 400, '重复用户名');
  });

  // ── 未认证访问受保护 API ──
  await reporter.run('GET /api/orders/my 未登录被拒绝', 'security-api', async () => {
    const { status } = await apiGet('/api/orders/my');
    assert(isAccessDenied(status), '未登录应 401/403/302');
  });

  await reporter.run('POST /api/categories 未登录被拒绝', 'security-api', async () => {
    const { status } = await apiPost('/api/categories', { name: '测试分类', description: 'x' });
    assert(isAccessDenied(status), '未登录创建分类应被拒绝');
  });

  // ── 角色授权（JWT Bearer + Session Cookie） ──
  let customerToken;
  let customerSession;

  await reporter.run('JWT 登录返回可用 Token', 'auth-api', async () => {
    customerToken = await loginWithJwt(ACCOUNTS.customer.username, ACCOUNTS.customer.password);
    assert(customerToken.length > 10, 'Token 长度应合理');
    return { tokenLength: customerToken.length };
  });

  await reporter.run('客户 JWT 可访问 GET /api/orders/my', 'role-api', async () => {
    const { status, body } = await apiGet('/api/orders/my', customerToken);
    assertStatus(status, 200, '客户订单列表');
    assert(Array.isArray(body), '订单列表应为数组');
    return { orderCount: body.length };
  });

  await reporter.run('客户 JWT 可访问 GET /api/recommendations/my', 'role-api', async () => {
    const { status, body } = await apiGet('/api/recommendations/my?topN=5', customerToken);
    assertStatus(status, 200, '推荐接口');
    assert(Array.isArray(body), '推荐结果应为 ID 数组');
    return { recommendationCount: body.length };
  });

  await reporter.run('客户 JWT 不可访问 GET /api/admin/orders', 'role-api', async () => {
    const { status } = await apiGet('/api/admin/orders', customerToken);
    assert(status === 403 || status === 401 || status >= 500, `客户访问管理订单应被拒绝，实际 ${status}`);
  });

  await reporter.run('商家 JWT 可访问 GET /api/admin/orders', 'role-api', async () => {
    const merchantToken = await loginWithJwt(ACCOUNTS.merchant.username, ACCOUNTS.merchant.password);
    const { status, body } = await apiGet('/api/admin/orders', merchantToken);
    assertStatus(status, 200, '商家订单 API');
    assert(Array.isArray(body), '响应应为数组');
    return { orderCount: body.length };
  });

  await reporter.run('管理员 JWT 可访问 GET /api/admin/orders', 'role-api', async () => {
    const adminToken = await loginWithJwt(ACCOUNTS.admin.username, ACCOUNTS.admin.password);
    const { status, body } = await apiGet('/api/admin/orders', adminToken);
    assertStatus(status, 200, '管理员订单 API');
    assert(Array.isArray(body), '响应应为数组');
    return { orderCount: body.length };
  });

  await reporter.run('商家 JWT POST /api/products 可达', 'role-api', async () => {
    const merchantToken = await loginWithJwt(ACCOUNTS.merchant.username, ACCOUNTS.merchant.password);
    const { status } = await apiPost(
      '/api/products?categoryId=1',
      {
        name: `api_smoke_${Date.now()}`,
        description: 'API smoke test product',
        price: 9.99,
        stock: 1,
        isAvailable: true,
      },
      merchantToken,
    );
    assert(status >= 200 && status < 500, `创建商品接口应可达，实际 ${status}`);
    return { status };
  });

  await reporter.run('客户 Session 可访问 GET /api/orders/my', 'role-api', async () => {
    customerSession = await loginWithSession(ACCOUNTS.customer.username, ACCOUNTS.customer.password);
    const { status, body } = await customerSession.get('/api/orders/my');
    assertStatus(status, 200, 'Session 客户订单列表');
    assert(Array.isArray(body), '订单列表应为数组');
    return { orderCount: body.length };
  });

  // ── 业务链路（只读 + 可选下单，会消耗库存） ──
  const enableOrderTest = process.env.ENABLE_ORDER_TEST === '1';
  if (enableOrderTest && products.length > 0) {
    await reporter.run('POST /api/orders 客户创建订单（ENABLE_ORDER_TEST=1）', 'business-api', async () => {
      if (!customerSession) {
        customerSession = await loginWithSession(ACCOUNTS.customer.username, ACCOUNTS.customer.password);
      }
      const product = products.find((p) => p.stock > 0 && p.isAvailable !== false) || products[0];
      const { status, body } = await customerSession.post('/api/orders', {
        items: [{ productId: product.id, quantity: 1 }],
      });
      assertStatus(status, [200, 201], '创建订单');
      assert(body?.id != null, '订单应有 id');
      return { orderId: body.id, productId: product.id };
    });
  } else {
    console.log('  · 跳过 POST /api/orders（设置 ENABLE_ORDER_TEST=1 可启用）');
  }

  const { passed, failed, total } = reporter.summary();
  const reportPath = await reporter.writeReport(`api-smoke_${Date.now()}.json`);

  console.log(`\nAPI 测试完成: ${passed}/${total} 通过, ${failed} 失败`);
  console.log(`报告: ${reportPath}`);

  if (failed > 0) process.exit(1);
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
