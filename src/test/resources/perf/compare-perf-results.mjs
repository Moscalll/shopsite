import fs from 'node:fs/promises';
import path from 'node:path';

const BEFORE = process.argv[2];
const AFTER = process.argv[3];

if (!BEFORE || !AFTER) {
  console.error('Usage: node compare-perf-results.mjs <before.json> <after.json>');
  process.exit(1);
}

async function loadJson(file) {
  return JSON.parse(await fs.readFile(file, 'utf8'));
}

function pct(before, after) {
  if (typeof before !== 'number' || typeof after !== 'number' || before === 0) return null;
  return ((after - before) / before) * 100;
}

function fmt(v) {
  return typeof v === 'number' ? v.toFixed(2) : 'N/A';
}

function getSummary(data, target, metric) {
  const item = (data.results || []).find((r) => r.target === target);
  return item?.summary?.[metric] ?? null;
}

async function main() {
  const before = await loadJson(BEFORE);
  const after = await loadJson(AFTER);
  const targets = Array.from(new Set([...(before.results || []).map((r) => r.target), ...(after.results || []).map((r) => r.target)]));

  const rows = targets.map((target) => ({
    target,
    fcpP50: {
      before: getSummary(before, target, 'fcpP50'),
      after: getSummary(after, target, 'fcpP50'),
    },
    lcpP50: {
      before: getSummary(before, target, 'lcpP50'),
      after: getSummary(after, target, 'lcpP50'),
    },
    clsP50: {
      before: getSummary(before, target, 'clsP50'),
      after: getSummary(after, target, 'clsP50'),
    },
    loadTimeP50: {
      before: getSummary(before, target, 'loadTimeP50'),
      after: getSummary(after, target, 'loadTimeP50'),
    },
    avgImageCount: {
      before: getSummary(before, target, 'avgImageCount'),
      after: getSummary(after, target, 'avgImageCount'),
    },
    avgImageSizeKB: {
      before: getSummary(before, target, 'avgImageSizeKB'),
      after: getSummary(after, target, 'avgImageSizeKB'),
    },
  })).map((row) => ({
    ...row,
    changes: Object.fromEntries(Object.entries(row).filter(([k]) => k !== 'target').map(([k, v]) => [k, pct(v.before, v.after)])),
  }));

  console.log('ShopSite 前后性能对比\n');
  for (const row of rows) {
    console.log(`页面: ${row.target}`);
    for (const [metric, value] of Object.entries(row)) {
      if (metric === 'target' || metric === 'changes') continue;
      console.log(`  ${metric}: before=${fmt(value.before)} after=${fmt(value.after)} change=${fmt(row.changes[metric])}%`);
    }
    console.log('');
  }

  const out = {
    beforeFile: path.resolve(BEFORE),
    afterFile: path.resolve(AFTER),
    generatedAt: new Date().toISOString(),
    rows,
  };
  const outFile = path.join(path.dirname(path.resolve(AFTER)), 'comparison-report.json');
  await fs.writeFile(outFile, JSON.stringify(out, null, 2), 'utf8');
  console.log(`Saved comparison report to ${outFile}`);
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
