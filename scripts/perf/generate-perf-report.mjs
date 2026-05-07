import fs from 'node:fs/promises';
import path from 'node:path';

const inputFile = process.argv[2];

if (!inputFile) {
  console.error('Usage: node scripts/perf/generate-perf-report.mjs <perf-json-file>');
  process.exit(1);
}

function fmt(v) {
  if (v === null || v === undefined) return '';
  if (typeof v === 'number') return Number.isInteger(v) ? String(v) : v.toFixed(1);
  return String(v);
}

function idSafe(input) {
  return String(input).replace(/[^a-zA-Z0-9]/g, '_');
}

function escapeHtml(input) {
  return String(input)
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;');
}

async function main() {
  const raw = await fs.readFile(inputFile, 'utf8');
  const data = JSON.parse(raw);

  const pages = data.results || [];
  const outDir = path.dirname(path.resolve(inputFile));
  const outFile = path.join(outDir, 'perf-report.html');

  const pageCards = pages
    .map((page) => {
      const details = page.details || [];
      const labels = details.map((_, idx) => `Run ${idx + 1}`);

      const fcp = details.map((d) => d.fcp ?? null);
      const lcp = details.map((d) => d.lcp ?? null);
      const loadTime = details.map((d) => d.totalLoadTime ?? null);
      const imageCount = details.map((d) => d.imageCount ?? null);
      const redirected = details.map((d) => (d.redirectedToLogin ? 1 : 0));
      const anomalyFlags = details.map((d) => {
        const suspicious =
          d.redirectedToLogin ||
          d.lcp === null ||
          (typeof d.totalLoadTime === 'number' && d.totalLoadTime < 200);
        return suspicious ? 1 : 0;
      });

      const rows = details
        .map(
          (d, idx) => `
      <tr>
        <td>${idx + 1}</td>
        <td>${d.url || ''}</td>
        <td>${d.finalUrl || ''}</td>
        <td>${d.redirectedToLogin ? 'Yes' : 'No'}</td>
        <td>${fmt(d.fcp)}</td>
        <td>${fmt(d.lcp)}</td>
        <td>${fmt(d.cls)}</td>
        <td>${fmt(d.totalLoadTime)}</td>
        <td>${fmt(d.imageCount)}</td>
        <td>${fmt(d.imageSizeKB)}</td>
        <td>${fmt(d.resourceCount)}</td>
      </tr>
    `,
        )
        .join('');

      const targetSafe = idSafe(page.target);

      return `
      <section class="card">
        <h2>${escapeHtml(page.target)}</h2>
        <p class="meta">
          Version: ${escapeHtml(page.version || data.version || '')}
          | Runs: ${page.runs || data.runs || ''}
          | Generated: ${escapeHtml(page.generatedAt || data.generatedAt || '')}
        </p>

        <div class="chart-grid">
          <div class="chart-box"><canvas id="fcp-${targetSafe}"></canvas></div>
          <div class="chart-box"><canvas id="lcp-${targetSafe}"></canvas></div>
          <div class="chart-box"><canvas id="load-${targetSafe}"></canvas></div>
          <div class="chart-box"><canvas id="img-${targetSafe}"></canvas></div>
        </div>

        <div class="chart-box full"><canvas id="flag-${targetSafe}"></canvas></div>

        <h3>Raw Runs</h3>
        <table>
          <thead>
            <tr>
              <th>#</th>
              <th>URL</th>
              <th>Final URL</th>
              <th>Redirected</th>
              <th>FCP</th>
              <th>LCP</th>
              <th>CLS</th>
              <th>LoadTime</th>
              <th>ImageCount</th>
              <th>ImageSizeKB</th>
              <th>ResourceCount</th>
            </tr>
          </thead>
          <tbody>${rows}</tbody>
        </table>

        <script>
          (function() {
            const labels = ${JSON.stringify(labels)};
            const fcp = ${JSON.stringify(fcp)};
            const lcp = ${JSON.stringify(lcp)};
            const loadTime = ${JSON.stringify(loadTime)};
            const imageCount = ${JSON.stringify(imageCount)};
            const redirected = ${JSON.stringify(redirected)};
            const anomalyFlags = ${JSON.stringify(anomalyFlags)};

            function drawLine(canvasId, label, dataset, color) {
              const ctx = document.getElementById(canvasId).getContext('2d');
              new Chart(ctx, {
                type: 'line',
                data: {
                  labels,
                  datasets: [{
                    label,
                    data: dataset,
                    borderColor: color,
                    backgroundColor: color + '22',
                    tension: 0.25,
                    spanGaps: true,
                    pointRadius: 4
                  }]
                },
                options: {
                  responsive: true,
                  scales: { y: { beginAtZero: true } }
                }
              });
            }

            drawLine('fcp-${targetSafe}', 'FCP', fcp, '#2563eb');
            drawLine('lcp-${targetSafe}', 'LCP', lcp, '#dc2626');
            drawLine('load-${targetSafe}', 'LoadTime', loadTime, '#16a34a');
            drawLine('img-${targetSafe}', 'ImageCount', imageCount, '#7c3aed');

            const flagCtx = document.getElementById('flag-${targetSafe}').getContext('2d');
            new Chart(flagCtx, {
              type: 'bar',
              data: {
                labels,
                datasets: [
                  { label: 'Redirected', data: redirected, backgroundColor: '#f59e0b' },
                  { label: 'Anomaly', data: anomalyFlags, backgroundColor: '#ef4444' }
                ]
              },
              options: {
                responsive: true,
                scales: {
                  y: {
                    beginAtZero: true,
                    max: 1,
                    ticks: { stepSize: 1 }
                  }
                }
              }
            });
          })();
        </script>
      </section>
    `;
    })
    .join('\n');

  const html = `<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>ShopSite Perf Report</title>
  <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
  <style>
    body { font-family: Arial, sans-serif; margin: 0; background: #f6f7fb; color: #111827; }
    header { background: #111827; color: white; padding: 20px 28px; }
    .container { max-width: 1400px; margin: 0 auto; padding: 24px; }
    .card { background: white; border-radius: 14px; padding: 20px; margin-bottom: 24px; box-shadow: 0 10px 24px rgba(0,0,0,.06); }
    .meta { color: #6b7280; font-size: 14px; margin-top: 4px; }
    .chart-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16px; margin: 16px 0; }
    .chart-box { background: #fafafa; border: 1px solid #e5e7eb; border-radius: 12px; padding: 12px; min-height: 320px; }
    .chart-box.full { margin: 16px 0; }
    table { width: 100%; border-collapse: collapse; margin-top: 16px; font-size: 14px; }
    th, td { border: 1px solid #e5e7eb; padding: 8px 10px; text-align: left; vertical-align: top; }
    th { background: #f3f4f6; }
  </style>
</head>
<body>
  <header>
    <h1>ShopSite 性能图表报告</h1>
    <div>数据文件: ${escapeHtml(path.basename(inputFile))}</div>
  </header>
  <div class="container">${pageCards}</div>
</body>
</html>`;

  await fs.writeFile(outFile, html, 'utf8');
  console.log(`Generated report: ${outFile}`);
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
