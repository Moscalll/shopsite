import { chromium } from 'playwright';
import fs from 'node:fs/promises';
import path from 'node:path';

const BASE_URL = process.env.BASE_URL || 'http://localhost:8080';
const OUT_DIR = process.env.OUT_DIR || 'perf-results';
const VERSION = process.env.VERSION || 'before-auth';
const RUNS = Number(process.env.RUNS || 5);
const ROLES = (process.env.PERF_ROLES || 'customer,merchant,admin')
  .split(',')
  .map((role) => role.trim().toLowerCase())
  .filter(Boolean);

const ROLE_CONFIG = {
  customer: {
    role: 'customer',
    username: process.env.CUSTOMER_USER || 'clientuser',
    password: process.env.CUSTOMER_PASS || 'ClientSecurePassword789',
    targets: ['/cart', '/checkout', '/orders', '/favorites', '/profile'],
  },
  merchant: {
    role: 'merchant',
    username: process.env.MERCHANT_USER || 'testmerchant',
    password: process.env.MERCHANT_PASS || 'testmerchantPASSWORD',
    targets: ['/merchant', '/merchant/orders', '/merchant/customers', '/merchant/statistics'],
  },
  admin: {
    role: 'admin',
    username: process.env.ADMIN_USER || 'platformadmin',
    password: process.env.ADMIN_PASS || 'AdminSecurePassword123',
    targets: ['/admin', '/admin/orders', '/admin/merchants', '/admin/products'],
  },
};

function nowIso() {
  return new Date().toISOString();
}

function safeName(input) {
  return input.replace(/[^a-zA-Z0-9-_]+/g, '_').replace(/^_+|_+$/g, '');
}

function percentile(values, p) {
  if (!values.length) return null;
  const sorted = [...values].sort((a, b) => a - b);
  const idx = Math.min(sorted.length - 1, Math.ceil((p / 100) * sorted.length) - 1);
  return sorted[idx];
}

async function ensureDir(dir) {
  await fs.mkdir(dir, { recursive: true });
}

async function login(page, username, password) {
  await page.goto(`${BASE_URL}/login`, { waitUntil: 'domcontentloaded' });
  await page.locator('input[name="username"]').fill(username);
  await page.locator('input[name="password"]').fill(password);

  await Promise.allSettled([
    page.waitForURL((url) => !url.href.includes('/login'), { timeout: 15000 }),
    page.waitForSelector('.alert-danger', { timeout: 15000 }),
    page.getByRole('button', { name: '登录' }).click(),
  ]);

  await page.waitForLoadState('domcontentloaded').catch(() => {});

  const currentUrl = page.url();
  if (currentUrl.includes('/login')) {
    const errorVisible = await page.locator('.alert-danger').isVisible().catch(() => false);
    if (errorVisible) {
      const msg = (await page.locator('.alert-danger').textContent().catch(() => '')) || '';
      throw new Error(`登录失败，服务器返回错误: ${msg.trim() || '未知错误'}`);
    }
    throw new Error(`登录未成功，仍停留在登录页。当前 URL: ${currentUrl}`);
  }
}

async function measurePage(page, url) {
  await page.goto(url, { waitUntil: 'domcontentloaded', timeout: 30000 });
  await page.waitForLoadState('load', { timeout: 30000 }).catch(() => {});
  await page.waitForTimeout(1000);

  const finalUrl = page.url();
  const redirectedToLogin = finalUrl.includes('/login');

  const metrics = await page.evaluate(() => {
    const nav = performance.getEntriesByType('navigation')[0];
    const paintEntries = performance.getEntriesByType('paint');
    const resourceEntries = performance.getEntriesByType('resource');
    const fcp = paintEntries.find((e) => e.name === 'first-contentful-paint')?.startTime ?? null;
    const lcpEntry = performance
      .getEntriesByType('largest-contentful-paint')
      .at(-1);

    return {
      fcp,
      lcp: lcpEntry?.startTime ?? fcp,
      domContentLoaded: nav?.domContentLoadedEventEnd ?? null,
      loadEventEnd: nav?.loadEventEnd ?? null,
      totalLoadTime: nav ? nav.loadEventEnd - nav.startTime : null,
      imageCount: resourceEntries.filter((e) => e.initiatorType === 'img').length,
      imageSizeKB: resourceEntries.reduce((sum, e) => sum + ((e.transferSize || 0) / 1024), 0),
      resourceCount: resourceEntries.length,
    };
  });

  return {
    url,
    finalUrl,
    redirectedToLogin,
    timestamp: new Date().toISOString(),
    ...metrics,
  };
}

async function measureRole(browser, roleConfig) {
  const context = await browser.newContext({
    viewport: { width: 1440, height: 900 },
    deviceScaleFactor: 1,
  });

  const page = await context.newPage();

  const client = await page.context().newCDPSession(page);
  await client.send('Network.emulateNetworkConditions', {
    offline: false,
    downloadThroughput: (1.5 * 1024 * 1024) / 8, // 1.5 Mbps
    uploadThroughput: (750 * 1024) / 8,      // 750 Kbps
    latency: 100,                             // 100ms RTT
  });

  await login(page, roleConfig.username, roleConfig.password);

  const roleResults = [];
  for (const target of roleConfig.targets) {
    const targetUrl = new URL(target, BASE_URL).toString();
    const runs = [];

    for (let i = 0; i < RUNS; i++) {
      runs.push(await measurePage(page, targetUrl));
    }

    const fcpValues = runs.map((r) => r.fcp).filter((v) => typeof v === 'number');
    const lcpValues = runs.map((r) => r.lcp).filter((v) => typeof v === 'number');
    const clsValues = runs.map((r) => r.cls).filter((v) => typeof v === 'number');
    const loadValues = runs.map((r) => r.totalLoadTime).filter((v) => typeof v === 'number');

    roleResults.push({
      version: VERSION,
      role: roleConfig.role,
      target: targetUrl,
      runs: RUNS,
      generatedAt: nowIso(),
      summary: {
        fcpP50: percentile(fcpValues, 50),
        lcpP50: percentile(lcpValues, 50),
        clsP50: percentile(clsValues, 50),
        loadTimeP50: percentile(loadValues, 50),
        avgImageCount: runs.reduce((sum, r) => sum + (r.imageCount || 0), 0) / runs.length,
        avgImageSizeKB: runs.reduce((sum, r) => sum + (r.imageSizeKB || 0), 0) / runs.length,
      },
      details: runs,
    });
  }

  await context.close();
  return roleResults;
}

async function main() {
  await ensureDir(OUT_DIR);
  const roleConfigs = ROLES.map((role) => ROLE_CONFIG[role]).filter(Boolean);

  if (!roleConfigs.length) {
    throw new Error(`PERF_ROLES 无效，可选值: ${Object.keys(ROLE_CONFIG).join(',')}`);
  }

  const browser = await chromium.launch({ headless: true });
  const allResults = [];

  for (const roleConfig of roleConfigs) {
    const result = await measureRole(browser, roleConfig);
    allResults.push(...result);
  }

  const output = {
    baseUrl: BASE_URL,
    version: VERSION,
    runs: RUNS,
    roles: roleConfigs.map((r) => r.role),
    generatedAt: nowIso(),
    results: allResults,
  };

  const fileName = `${safeName(VERSION)}_auth_${Date.now()}.json`;
  const filePath = path.join(OUT_DIR, fileName);
  await fs.writeFile(filePath, JSON.stringify(output, null, 2), 'utf8');
  await browser.close();

  console.log(`Saved auth performance metrics to ${filePath}`);
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
