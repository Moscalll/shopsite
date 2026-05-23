<#
.SYNOPSIS
  一键：拉库 -> LLM 新品 -> prompts/catalog ->（确认）-> ComfyUI 出图 ->（确认）-> 导入 MySQL + uploads

.PARAMETER MaxProducts
  可选硬上限：传给 generate_candidates_llm.py --max-products。
  省略或传 0 表示不按此参数截断，条数完全由 config.yaml 里
  items_per_category / items_per_category_each / items_per_category_by_id
  与 allowed_category_ids 决定。

.PARAMETER SkipRender
  跳过 ComfyUI 出图（仅生成 product_spec / prompts / catalog）。

.PARAMETER SkipImport
  跳过数据库导入（需已出图且 SkipRender 为 false，或你自行准备 images）。

.PARAMETER NonInteractive
  不暂停确认（适合脚本/CI）；默认在 prompts 与出图后各暂停一次 Read-Host。

.PARAMETER ItemsPerCategory
  覆盖 config：每个 allowed 类目各生成 N 条（>0 时传给 --items-per-category）。

.PARAMETER ItemsPerCategoryEach
  覆盖 config：逗号分隔整数列表，传给 --items-per-category-each（与 allowed_category_ids 等长）。

.PARAMETER ItemsPerCategoryById
  覆盖 config：形如 6=2,7=3，传给 --items-per-category-by-id。以上三项与 ItemsPerCategory 系互斥，只可填其一。

前置：tools\.env、tools\pipeline\config.yaml；出图需 ComfyUI + image_render\config.yaml；导入需 pip pymysql。
#>
[CmdletBinding()]
param(
    [Parameter()][int]$MaxProducts = 0,
    [Parameter()][int]$ItemsPerCategory = 0,
    [Parameter()][string]$ItemsPerCategoryEach = "",
    [Parameter()][string]$ItemsPerCategoryById = "",
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
if ($MaxProducts -lt 0) {
    Write-Error "-MaxProducts must be >= 0 (0 = use config only, omit --max-products)"
    exit 1
}

$ipcOpts = 0
if ($ItemsPerCategory -gt 0) { $ipcOpts++ }
if (-not [string]::IsNullOrWhiteSpace($ItemsPerCategoryEach)) { $ipcOpts++ }
if (-not [string]::IsNullOrWhiteSpace($ItemsPerCategoryById)) { $ipcOpts++ }
if ($ipcOpts -gt 1) {
    Write-Error "只可指定其一: -ItemsPerCategory / -ItemsPerCategoryEach / -ItemsPerCategoryById"
    exit 1
}

Write-Host "== 1) Fetch categories + existing products (MySQL) ==" -ForegroundColor Cyan
python .\pipeline\fetch_existing.py --config .\pipeline\config.yaml
Test-Step "fetch_existing"

$genArgs = @(".\pipeline\generate_candidates_llm.py", "--config", ".\pipeline\config.yaml")
if ($MaxProducts -gt 0) { $genArgs += @("--max-products", "$MaxProducts") }
if ($ItemsPerCategory -gt 0) {
    $genArgs += @("--items-per-category", "$ItemsPerCategory")
} elseif (-not [string]::IsNullOrWhiteSpace($ItemsPerCategoryEach)) {
    $genArgs += @("--items-per-category-each", $ItemsPerCategoryEach.Trim())
} elseif (-not [string]::IsNullOrWhiteSpace($ItemsPerCategoryById)) {
    $genArgs += @("--items-per-category-by-id", $ItemsPerCategoryById.Trim())
}

$genLabel = "== 2) LLM generate product_spec =="
if ($MaxProducts -gt 0) { $genLabel += " [--max-products $MaxProducts]" }
if ($ItemsPerCategory -gt 0) { $genLabel += " [--items-per-category $ItemsPerCategory]" }
elseif (-not [string]::IsNullOrWhiteSpace($ItemsPerCategoryEach)) { $genLabel += " [--items-per-category-each ...]" }
elseif (-not [string]::IsNullOrWhiteSpace($ItemsPerCategoryById)) { $genLabel += " [--items-per-category-by-id ...]" }
else { $genLabel += " (条数默认来自 config.yaml，未传每类 CLI 覆盖)" }
Write-Host $genLabel -ForegroundColor Cyan
& python @genArgs
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
