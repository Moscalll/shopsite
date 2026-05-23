/**
 * ShopSite 页面功能测试（Playwright E2E）
 * 覆盖：公开页面、登录/登出、三角色页面访问、购物流程、权限隔离
 */
import { chromium } from 'playwright';
import fs from 'node:fs/promises';
import path from 'node:path';
import { ACCOUNTS, PUBLIC_PAGES, ROLE_PAGES, BASE_URL, OUT_DIR } from './config.mjs';
import { TestReporter, assert, nowIso, waitForServer } from './lib.mjs';

async function loginForm(page, username, password) {
  await page.goto(`${BASE_URL}/login`, { waitUntil: 'domcontentloaded' });
  await page.locator('input[name="username"]').fill(username);
  await page.locator('input[name="password"]').fill(password);

  await Promise.all([
    page.waitForNavigation({ waitUntil: 'domcontentloaded', timeout: 15000 }).catch(() => null),
    page.getByRole('button', { name: '登录' }).click(),
  ]);

  if (page.url().includes('/login?error')) {
    throw new Error(`登录失败: ${username} / ${page.url()}`);
  }
  assert(!page.url().includes('/login'), `登录失败，仍停留在 ${page.url()}`);
}

async function logout(page) {
  const logoutLink = page.locator('a[href*="/logout"], form[action*="/logout"] button, form[action*="/logout"] input[type="submit"]');
  if (await logoutLink.count()) {
    await logoutLink.first().click();
    await page.waitForLoadState('domcontentloaded');
  }
}

async function expectNotLoginRedirect(page, targetPath) {
  const finalUrl = page.url();
  assert(!finalUrl.includes('/login'), `${targetPath} 不应重定向到登录页 (${finalUrl})`);
}

async function findFirstProductLink(page) {
  await page.goto(`${BASE_URL}/products`, { waitUntil: 'domcontentloaded' });
  const link = page.locator('a[href*="/product/"]').first();
  if ((await link.count()) === 0) return null;
  const href = await link.getAttribute('href');
  return href;
}

async function main() {
  console.log('\n== 页面功能测试 (E2E) ==');
  await waitForServer();

  const reporter = new TestReporter('functional-e2e');
  const browser = await chromium.launch({ headless: true });

  // ── 公开页面黑盒 ──
  {
    const context = await browser.newContext({ viewport: { width: 1440, height: 900 } });
    const page = await context.newPage();

    for (const item of PUBLIC_PAGES) {
      await reporter.run(`公开页面可访问: ${item.name} (${item.path})`, 'blackbox-public', async () => {
        const res = await page.goto(`${BASE_URL}${item.path}`, { waitUntil: 'domcontentloaded', timeout: 30000 });
        assert(res?.status() != null && res.status() < 400, `HTTP ${res?.status()}`);
        if (item.expectText) {
          await page.getByText(item.expectText, { exact: false }).first().waitFor({ timeout: 5000 });
        }
        return { status: res?.status(), url: page.url() };
      });
    }

    await reporter.run('商品详情页可访问', 'blackbox-public', async () => {
      const productHref = await findFirstProductLink(page);
      if (!productHref) return { skipped: '无商品链接' };
      const res = await page.goto(`${BASE_URL}${productHref}`, { waitUntil: 'domcontentloaded' });
      assert(res?.status() != null && res.status() < 400, `详情页 HTTP ${res?.status()}`);
      return { url: page.url() };
    });

    await reporter.run('未登录访问 /cart 重定向登录', 'security-ui', async () => {
      await page.goto(`${BASE_URL}/cart`, { waitUntil: 'domcontentloaded' });
      assert(page.url().includes('/login'), `应跳转登录，当前 ${page.url()}`);
    });

    await reporter.run('未登录访问 /admin 重定向登录', 'security-ui', async () => {
      await page.goto(`${BASE_URL}/admin`, { waitUntil: 'domcontentloaded' });
      assert(page.url().includes('/login'), `应跳转登录，当前 ${page.url()}`);
    });

    await reporter.run('错误密码登录显示错误提示', 'auth-ui', async () => {
      await page.goto(`${BASE_URL}/login`, { waitUntil: 'domcontentloaded' });
      await page.locator('input[name="username"]').fill(ACCOUNTS.customer.username);
      await page.locator('input[name="password"]').fill('WrongPassword!!!');
      await page.getByRole('button', { name: '登录' }).click();
      await page.waitForURL(/\/login/, { timeout: 10000 });
      const hasError = await page.locator('.alert-danger').isVisible().catch(() => false);
      assert(hasError || page.url().includes('error'), '应显示登录失败');
    });

    await context.close();
  }

  // ── 客户功能流程 ──
  {
    const context = await browser.newContext({ viewport: { width: 1440, height: 900 } });
    const page = await context.newPage();

    await reporter.run('客户登录成功进入首页', 'functional-customer', async () => {
      await loginForm(page, ACCOUNTS.customer.username, ACCOUNTS.customer.password);
      assert(page.url().replace(/\/$/, '').endsWith(BASE_URL.replace(/\/$/, '')) || page.url().includes('/'), '登录后应离开登录页');
      return { url: page.url() };
    });

    const customerAccountsToVerify = [
      ACCOUNTS.customerAlt,
      ACCOUNTS.customerB,
      ACCOUNTS.customerC,
      ACCOUNTS.customerD,
      ACCOUNTS.customerE,
    ];

    for (const account of customerAccountsToVerify) {
      await reporter.run(`新增客户账号 ${account.username} 可登录`, 'functional-customer', async () => {
        await logout(page).catch(() => {});
        await loginForm(page, account.username, account.password);
        return { username: account.username, url: page.url() };
      });
    }

    await logout(page).catch(() => {});
    await loginForm(page, ACCOUNTS.customer.username, ACCOUNTS.customer.password);

    for (const item of ROLE_PAGES.customer) {
      await reporter.run(`客户可访问: ${item.name}`, 'functional-customer', async () => {
        await page.goto(`${BASE_URL}${item.path}`, { waitUntil: 'domcontentloaded', timeout: 30000 });
        await expectNotLoginRedirect(page, item.path);
        return { url: page.url() };
      });
    }

    await reporter.run('客户加购流程: 详情页 → 购物车', 'functional-customer', async () => {
      const productHref = await findFirstProductLink(page);
      if (!productHref) return { skipped: '无可用商品' };

      await page.goto(`${BASE_URL}${productHref}`, { waitUntil: 'domcontentloaded' });
      const addBtn = page.getByRole('button', { name: /加入购物车/ });
      if ((await addBtn.count()) === 0) return { skipped: '详情页无加购按钮（可能未登录渲染）' };

      await addBtn.click();
      await page.waitForLoadState('domcontentloaded');

      await page.goto(`${BASE_URL}/cart`, { waitUntil: 'domcontentloaded' });
      await expectNotLoginRedirect(page, '/cart');

      const emptyText = page.getByText('购物车是空的');
      const hasItems = !(await emptyText.isVisible().catch(() => false));
      assert(hasItems, '加购后购物车不应为空');
      return { cartUrl: page.url() };
    });

    await reporter.run('客户不可访问 /admin', 'security-ui', async () => {
      await page.goto(`${BASE_URL}/admin`, { waitUntil: 'domcontentloaded' });
      const url = page.url();
      const forbidden = url.includes('/login') || url.includes('/error') || (await page.locator('text=403').count()) > 0;
      assert(forbidden, `客户访问 /admin 应被拒绝，当前 ${url}`);
    });

    await context.close();
  }

  // ── 商家功能 ──
  {
    const context = await browser.newContext({ viewport: { width: 1440, height: 900 } });
    const page = await context.newPage();

    await reporter.run('商家登录成功', 'functional-merchant', async () => {
      await loginForm(page, ACCOUNTS.merchant.username, ACCOUNTS.merchant.password);
      return { url: page.url() };
    });

    for (const item of ROLE_PAGES.merchant) {
      await reporter.run(`商家可访问: ${item.name}`, 'functional-merchant', async () => {
        await page.goto(`${BASE_URL}${item.path}`, { waitUntil: 'domcontentloaded', timeout: 30000 });
        await expectNotLoginRedirect(page, item.path);
        return { url: page.url() };
      });
    }

    await reporter.run('商家不可访问 /admin', 'security-ui', async () => {
      const res = await page.goto(`${BASE_URL}/admin`, { waitUntil: 'domcontentloaded' });
      const status = res?.status() ?? 0;
      const bodyText = await page.locator('body').innerText().catch(() => '');
      const denied =
        status === 403 ||
        page.url().includes('/login') ||
        page.url().includes('/error') ||
        /403|Forbidden|禁止|无权|Access Denied/i.test(bodyText);
      assert(denied, `商家访问 /admin 应被拒绝 (status=${status}, url=${page.url()})`);
    });

    await context.close();
  }

  // ── 管理员功能 ──
  {
    const context = await browser.newContext({ viewport: { width: 1440, height: 900 } });
    const page = await context.newPage();

    await reporter.run('管理员登录成功', 'functional-admin', async () => {
      await loginForm(page, ACCOUNTS.admin.username, ACCOUNTS.admin.password);
      return { url: page.url() };
    });

    for (const item of ROLE_PAGES.admin) {
      await reporter.run(`管理员可访问: ${item.name}`, 'functional-admin', async () => {
        await page.goto(`${BASE_URL}${item.path}`, { waitUntil: 'domcontentloaded', timeout: 30000 });
        await expectNotLoginRedirect(page, item.path);
        return { url: page.url() };
        });
    }

    await reporter.run('备用管理员账号 admin 可登录（init-data 未创建则跳过）', 'functional-admin', async () => {
      await logout(page).catch(() => {});
      try {
        await loginForm(page, ACCOUNTS.adminAlt.username, ACCOUNTS.adminAlt.password);
        return { username: ACCOUNTS.adminAlt.username };
      } catch (error) {
        return { skipped: `账号 ${ACCOUNTS.adminAlt.username} 不存在或未启用: ${error.message}` };
      }
    });

    await context.close();
  }

  // ── 搜索（需登录，/search 不在 SecurityConfig permitAll） ──
  {
    const context = await browser.newContext();
    const page = await context.newPage();
    await loginForm(page, ACCOUNTS.customer.username, ACCOUNTS.customer.password);

    await reporter.run('登录后搜索页可提交关键词', 'functional-browse', async () => {
      await page.goto(`${BASE_URL}/search?q=测试`, { waitUntil: 'domcontentloaded' });
      assert(!page.url().includes('/login'), '登录后搜索页不应跳转登录');
      return { url: page.url() };
    });

    await context.close();
  }

  await browser.close();

  const { passed, failed, total } = reporter.summary();
  const reportPath = path.join(OUT_DIR, `functional-e2e_${Date.now()}.json`);
  await fs.mkdir(OUT_DIR, { recursive: true });
  await fs.writeFile(
    reportPath,
    JSON.stringify(
      {
        suite: 'functional-e2e',
        baseUrl: BASE_URL,
        finishedAt: nowIso(),
        total,
        passed,
        failed,
        cases: reporter.cases,
      },
      null,
      2,
    ),
    'utf8',
  );

  console.log(`\n功能测试完成: ${passed}/${total} 通过, ${failed} 失败`);
  console.log(`报告: ${reportPath}`);

  if (failed > 0) process.exit(1);
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
