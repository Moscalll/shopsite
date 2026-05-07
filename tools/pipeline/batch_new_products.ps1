<#
.SYNOPSIS
  一键：拉库 -> LLM 新品 -> prompts/catalog ->（确认）-> ComfyUI 出图 ->（确认）-> 导入 MySQL + uploads

.PARAMETER MaxProducts
  最多生成多少条商品（传给 generate_candidates_llm.py --max-products）。默认 8。

.PARAMETER SkipRender
  跳过 ComfyUI 出图（仅生成 product_spec / prompts / catalog）。

.PARAMETER SkipImport
  跳过数据库导入（需已出图且 SkipRender 为 false，或你自行准备 images）。

.PARAMETER NonInteractive
  不暂停确认（适合脚本/CI）；默认在 prompts 与出图后各暂停一次 Read-Host。

前置：tools\.env、tools\pipeline\config.yaml；出图需 ComfyUI + image_render\config.yaml；导入需 pip pymysql。
#>
[CmdletBinding()]
param(
    [Parameter()][int]$MaxProducts = 8,
    [switch]$SkipRender,
    [switch]$SkipImport,
    [switch]$NonInteractive
)

$ErrorActionPreference = "Stop"
Set-Location (Join-Path $PSScriptRoot "..")

function Test-Step {
    param([string]$Label)
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Step failed ($Label), exit code $LASTEXITCODE"
        exit $LASTEXITCODE
    }
}

if (-not (Test-Path ".\.env")) {
    Write-Error "Missing tools\.env — copy from .env.example and set MYSQL_* and SHOPSITE_UPLOAD_DIR."
    exit 1
}
if (-not (Test-Path ".\pipeline\config.yaml")) {
    Write-Error "Missing pipeline\config.yaml — copy from pipeline\config.example.yaml."
    exit 1
}
if ($MaxProducts -lt 1) {
    Write-Error "-MaxProducts must be >= 1"
    exit 1
}

Write-Host "== 1) Fetch categories + existing products (MySQL) ==" -ForegroundColor Cyan
python .\pipeline\fetch_existing.py --config .\pipeline\config.yaml
Test-Step "fetch_existing"

Write-Host "== 2) LLM generate product_spec (--max-products $MaxProducts) ==" -ForegroundColor Cyan
python .\pipeline\generate_candidates_llm.py --config .\pipeline\config.yaml --max-products $MaxProducts
Test-Step "generate_candidates_llm"

Write-Host "== 3) Build prompts.jsonl + catalog.jsonl ==" -ForegroundColor Cyan
New-Item -ItemType Directory -Force -Path ".\out\pipeline_run\generated\images" | Out-Null
python .\product_pipeline\generate.py --in .\out\pipeline_run\product_spec.auto.jsonl --out-dir .\out\pipeline_run\generated
Test-Step "generate.py"

$prompts = (Resolve-Path ".\out\pipeline_run\generated\prompts.jsonl").Path
$catalog = (Resolve-Path ".\out\pipeline_run\generated\catalog.jsonl").Path
Write-Host "Prompts:  $prompts"
Write-Host "Catalog: $catalog"

if (-not $NonInteractive) {
    Write-Host ""
    Read-Host "Review prompts/catalog, then press Enter to continue (or Ctrl+C to stop)"
}

if ($SkipRender) {
    Write-Host "== 4) Skipped render (-SkipRender) ==" -ForegroundColor Yellow
} else {
    if (-not (Test-Path ".\image_render\config.yaml")) {
        Write-Error "Missing image_render\config.yaml — copy from image_render\config.example.yaml and set comfyui workflow + node ids."
        exit 1
    }
    Write-Host "== 4) Render images (ComfyUI / backend in config) ==" -ForegroundColor Cyan
    Write-Host "Ensure ComfyUI is running if backend=comfyui."
    python .\image_render\render_batch.py --prompts .\out\pipeline_run\generated\prompts.jsonl --images-dir .\out\pipeline_run\generated\images --config .\image_render\config.yaml
    Test-Step "render_batch"
    if (-not $NonInteractive) {
        Write-Host ""
        Read-Host "Review images under out\pipeline_run\generated\images, then press Enter to import (or Ctrl+C to stop)"
    }
}

if ($SkipImport) {
    Write-Host "== 5) Skipped import (-SkipImport) ==" -ForegroundColor Yellow
    Write-Host "Done. Manual import:"
    Write-Host "  python db_import\import_products.py --catalog out\pipeline_run\generated\catalog.jsonl --images-dir out\pipeline_run\generated\images --skip-if-name-exists"
    exit 0
}

Write-Host "== 5) Check pymysql ==" -ForegroundColor Cyan
python -c "import pymysql" 2>$null
if ($LASTEXITCODE -ne 0) {
    Write-Error "pymysql not installed. Run: pip install -r requirements.txt"
    exit 1
}

Write-Host "== 6) Import MySQL + copy to uploads ==" -ForegroundColor Cyan
python .\db_import\import_products.py --catalog .\out\pipeline_run\generated\catalog.jsonl --images-dir .\out\pipeline_run\generated\images --skip-if-name-exists
Test-Step "import_products"

Write-Host ""
Write-Host "All steps finished." -ForegroundColor Green
