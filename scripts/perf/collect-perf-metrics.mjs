import { chromium } from 'playwright';
import fs from 'node:fs/promises';
import path from 'node:path';

const BASE_URL = process.env.BASE_URL || 'http://localhost:8080';
const OUT_DIR = process.env.OUT_DIR || 'perf-results';
const VERSION = process.env.VERSION || 'before';
const RUNS = Number(process.env.RUNS || 5);
const TARGETS = (process.env.TARGETS || '/,/products,/product/1').split(',');

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

async function measurePage(browser, url) {
  const context = await browser.newContext({
    viewport: { width: 1440, height: 900 },
    deviceScaleFactor: 1,
  });
  const page = await context.newPage();

  await page.goto(url, { waitUntil: 'domcontentloaded' });

  const observerEnabled = await page.evaluate(() => {
    window.__perf = {
      lcp: null,
      cls: 0,
      clsEntries: [],
      lcpEntries: [],
    };

    const lcpObserver = new PerformanceObserver((list) => {
      const entries = list.getEntries();
      const last = entries[entries.length - 1];
      if (last) {
        window.__perf.lcp = last.startTime;
        window.__perf.lcpEntries.push({
          startTime: last.startTime,
          size: last.size,
          element: last.element ? last.element.tagName : null,
          url: last.url || null,
        });
      }
    });

    const clsObserver = new PerformanceObserver((list) => {
      for (const entry of list.getEntries()) {
        if (!entry.hadRecentInput) {
          window.__perf.cls += entry.value;
          window.__perf.clsEntries.push({
            value: entry.value,
            startTime: entry.startTime,
            sources: (entry.sources || []).length,
          });
        }
      }
    });

    try {
      lcpObserver.observe({ type: 'largest-contentful-paint', buffered: true });
      clsObserver.observe({ type: 'layout-shift', buffered: true });
      return true;
    } catch (_error) {
      return false;
    }
  });

  await page.waitForLoadState('networkidle');
  await page.waitForTimeout(500);

  const metrics = await page.evaluate(() => {
    const nav = performance.getEntriesByType('navigation')[0];
    const paintEntries = performance.getEntriesByType('paint');
    const resourceEntries = performance.getEntriesByType('resource');
    const perf = window.__perf || {};

    const fcp = paintEntries.find((e) => e.name === 'first-contentful-paint')?.startTime ?? null;
    const lcp = perf.lcp ?? null;
    const cls = perf.cls ?? 0;

    const imageResources = resourceEntries.filter(
      (e) => e.initiatorType === 'img' || (e.name && /\.(png|jpg|jpeg|webp|avif|gif)(\?|$)/i.test(e.name)),
    );

    return {
      fcp,
      lcp,
      cls,
      clsEntries: perf.clsEntries || [],
      lcpEntries: perf.lcpEntries || [],
      domContentLoaded: nav?.domContentLoadedEventEnd ?? null,
      loadEventEnd: nav?.loadEventEnd ?? null,
      totalLoadTime: nav ? nav.loadEventEnd - nav.startTime : null,
      imageCount: imageResources.length,
      imageSizeKB: imageResources.reduce((sum, e) => sum + ((e.transferSize || 0) / 1024), 0),
      resourceCount: resourceEntries.length,
    };
  });

  const finalUrl = page.url();
  await context.close();

  return {
    url,
    finalUrl,
    redirectedToLogin: finalUrl.includes('/login'),
    observerEnabled,
    timestamp: nowIso(),
    ...metrics,
  };
}

async function main() {
  await ensureDir(OUT_DIR);
  const browser = await chromium.launch({ headless: true });
  const results = [];

  for (const target of TARGETS) {
    const targetUrl = new URL(target, BASE_URL).toString();
    const pageRuns = [];

    for (let i = 0; i < RUNS; i++) {
      pageRuns.push(await measurePage(browser, targetUrl));
    }

    const fcpValues = pageRuns.map((r) => r.fcp).filter((v) => typeof v === 'number');
    const lcpValues = pageRuns.map((r) => r.lcp).filter((v) => typeof v === 'number');
    const clsValues = pageRuns.map((r) => r.cls).filter((v) => typeof v === 'number');
    const loadValues = pageRuns.map((r) => r.totalLoadTime).filter((v) => typeof v === 'number');

    results.push({
      version: VERSION,
      target: targetUrl,
      runs: RUNS,
      generatedAt: nowIso(),
      summary: {
        fcpP50: percentile(fcpValues, 50),
        lcpP50: percentile(lcpValues, 50),
        clsP50: percentile(clsValues, 50),
        loadTimeP50: percentile(loadValues, 50),
        avgImageCount: pageRuns.reduce((sum, r) => sum + (r.imageCount || 0), 0) / pageRuns.length,
        avgImageSizeKB: pageRuns.reduce((sum, r) => sum + (r.imageSizeKB || 0), 0) / pageRuns.length,
      },
      details: pageRuns,
    });
  }

  const output = {
    baseUrl: BASE_URL,
    version: VERSION,
    runs: RUNS,
    generatedAt: nowIso(),
    results,
  };

  const fileName = `${safeName(VERSION)}_${Date.now()}.json`;
  const filePath = path.join(OUT_DIR, fileName);
  await fs.writeFile(filePath, JSON.stringify(output, null, 2), 'utf8');
  await browser.close();

  console.log(`Saved performance metrics to ${filePath}`);
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
