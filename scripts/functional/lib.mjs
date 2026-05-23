import fs from 'node:fs/promises';
import path from 'node:path';
import { BASE_URL, OUT_DIR } from './config.mjs';

export function nowIso() {
  return new Date().toISOString();
}

export async function ensureDir(dir) {
  await fs.mkdir(dir, { recursive: true });
}

export function url(pathname) {
  return new URL(pathname, BASE_URL).toString();
}

export class TestReporter {
  constructor(suiteName) {
    this.suiteName = suiteName;
    this.startedAt = nowIso();
    this.cases = [];
  }

  async run(name, category, fn) {
    const started = Date.now();
    try {
      const detail = await fn();
      this.cases.push({
        name,
        category,
        status: 'passed',
        durationMs: Date.now() - started,
        detail: detail ?? null,
      });
      console.log(`  ✓ ${name}`);
      return true;
    } catch (error) {
      this.cases.push({
        name,
        category,
        status: 'failed',
        durationMs: Date.now() - started,
        error: error?.message || String(error),
      });
      console.error(`  ✗ ${name}: ${error?.message || error}`);
      return false;
    }
  }

  summary() {
    const passed = this.cases.filter((c) => c.status === 'passed').length;
    const failed = this.cases.filter((c) => c.status === 'failed').length;
    return { total: this.cases.length, passed, failed };
  }

  async writeReport(fileName) {
    await ensureDir(OUT_DIR);
    const report = {
      suite: this.suiteName,
      baseUrl: BASE_URL,
      startedAt: this.startedAt,
      finishedAt: nowIso(),
      ...this.summary(),
      cases: this.cases,
    };
    const filePath = path.join(OUT_DIR, fileName);
    await fs.writeFile(filePath, JSON.stringify(report, null, 2), 'utf8');
    return filePath;
  }
}

export function assert(condition, message) {
  if (!condition) throw new Error(message);
}

export function assertStatus(actual, expected, message) {
  const allowed = Array.isArray(expected) ? expected : [expected];
  assert(allowed.includes(actual), `${message} (status=${actual}, expected=${allowed.join('|')})`);
}

/** 通过 JWT 登录 API 获取 Bearer Token */
export async function loginWithJwt(username, password) {
  const res = await fetch(url('/api/auth/login'), {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
    body: JSON.stringify({ username, password }),
  });
  const body = await res.text();
  if (!res.ok) {
    throw new Error(`JWT 登录失败 (${res.status}): ${body}`);
  }
  const match = body.match(/Token:\s*(.+)$/);
  assert(match?.[1], `无法从登录响应解析 Token: ${body}`);
  return match[1].trim();
}

/** 表单登录获取 Session Cookie（与 Thymeleaf 页面流一致） */
export class SessionClient {
  constructor() {
    this.cookies = new Map();
  }

  ingest(response) {
    const setCookies = typeof response.headers.getSetCookie === 'function'
      ? response.headers.getSetCookie()
      : [];
    if (setCookies.length === 0) {
      const raw = response.headers.get('set-cookie');
      if (raw) setCookies.push(...raw.split(/,(?=[^;]+?=)/));
    }
    for (const item of setCookies) {
      const [pair] = item.split(';');
      const eq = pair.indexOf('=');
      if (eq > 0) {
        this.cookies.set(pair.slice(0, eq).trim(), pair.slice(eq + 1).trim());
      }
    }
  }

  cookieHeader() {
    return [...this.cookies.entries()].map(([k, v]) => `${k}=${v}`).join('; ');
  }

  async login(username, password) {
    const pageRes = await fetch(url('/login'), {
      redirect: 'manual',
      headers: this.cookieHeader() ? { Cookie: this.cookieHeader() } : {},
    });
    this.ingest(pageRes);
    const html = await pageRes.text();
    const csrf = html.match(/name="_csrf"\s+value="([^"]+)"/)?.[1]
      || html.match(/name="([^"]*csrf[^"]*)"\s+value="([^"]+)"/i)?.[2];

    const body = new URLSearchParams({ username, password });
    if (csrf) body.set('_csrf', csrf);

    const loginRes = await fetch(url('/login'), {
      method: 'POST',
      redirect: 'manual',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
        Cookie: this.cookieHeader(),
      },
      body,
    });
    this.ingest(loginRes);
    assert(loginRes.status === 302 || loginRes.status === 200, `表单登录失败 status=${loginRes.status}`);
    return this;
  }

  async get(pathname) {
    return apiGet(pathname, null, this.cookieHeader());
  }

  async post(pathname, payload) {
    return apiPost(pathname, payload, null, this.cookieHeader());
  }
}

export async function loginWithSession(username, password) {
  const client = new SessionClient();
  await client.login(username, password);
  return client;
}

export async function apiGet(pathname, token, cookieHeader) {
  const headers = { Accept: 'application/json' };
  if (token) headers.Authorization = `Bearer ${token}`;
  if (cookieHeader) headers.Cookie = cookieHeader;
  const res = await fetch(url(pathname), { headers, redirect: 'manual' });
  const text = await res.text();
  let body = text;
  if (text && (res.headers.get('content-type') || '').includes('application/json')) {
    try {
      body = JSON.parse(text);
    } catch {
      body = text;
    }
  }
  return { status: res.status, body, headers: res.headers };
}

export async function apiPost(pathname, payload, token, cookieHeader) {
  const headers = { 'Content-Type': 'application/json', Accept: 'application/json' };
  if (token) headers.Authorization = `Bearer ${token}`;
  if (cookieHeader) headers.Cookie = cookieHeader;
  const res = await fetch(url(pathname), {
    method: 'POST',
    headers,
    body: JSON.stringify(payload),
    redirect: 'manual',
  });
  const text = await res.text();
  let body = text;
  if (text && (res.headers.get('content-type') || '').includes('application/json')) {
    try {
      body = JSON.parse(text);
    } catch {
      body = text;
    }
  }
  return { status: res.status, body };
}

/** 检查站点是否在线 */
export async function waitForServer(timeoutMs = 30000) {
  const deadline = Date.now() + timeoutMs;
  let lastError = null;
  while (Date.now() < deadline) {
    try {
      const res = await fetch(url('/'), { redirect: 'manual' });
      if (res.status >= 200 && res.status < 500) return true;
    } catch (error) {
      lastError = error;
    }
    await new Promise((r) => setTimeout(r, 1000));
  }
  throw new Error(`站点 ${BASE_URL} 在 ${timeoutMs}ms 内不可达: ${lastError?.message || 'unknown'}`);
}

/** 未认证访问应被拒绝（401/403/302 跳转登录） */
export function isAccessDenied(status) {
  return status === 401 || status === 403 || status === 302;
}
