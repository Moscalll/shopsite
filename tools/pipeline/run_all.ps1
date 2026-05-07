$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot\..

if (-not (Test-Path ".\pipeline\config.yaml")) {
    Copy-Item ".\pipeline\config.example.yaml" ".\pipeline\config.yaml"
    Write-Host "Created pipeline\config.yaml — edit merchant_id / paths if needed."
}

Write-Host "== 1) Fetch categories + existing products (MySQL) =="
python .\pipeline\fetch_existing.py --config .\pipeline\config.yaml
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "== 2) LLM generate product_spec.auto.jsonl =="
python .\pipeline\generate_candidates_llm.py --config .\pipeline\config.yaml
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "== 3) Build prompts.jsonl + catalog.jsonl =="
New-Item -ItemType Directory -Force -Path ".\out\pipeline_run\generated\images" | Out-Null
python .\product_pipeline\generate.py --in .\out\pipeline_run\product_spec.auto.jsonl --out-dir .\out\pipeline_run\generated
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "== 4) Auto render images (optional) =="
if ($env:SHOPSITE_AUTO_RENDER -eq "1") {
    if (-not (Test-Path ".\image_render\config.yaml")) {
        Copy-Item ".\image_render\config.example.yaml" ".\image_render\config.yaml"
        Write-Host "Created image_render\config.yaml — set POLLINATIONS_API_KEY or comfyui workflow."
    }
    python .\image_render\render_batch.py --prompts .\out\pipeline_run\generated\prompts.jsonl --images-dir .\out\pipeline_run\generated\images --config .\image_render\config.yaml
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
} else {
    Write-Host "Skipped (set env SHOPSITE_AUTO_RENDER=1 to run image_render\render_batch.py)."
}

Write-Host ""
Write-Host "Done through step 3 (and step 4 if SHOPSITE_AUTO_RENDER=1)."
Write-Host "Manual render: python image_render\render_batch.py --prompts out\pipeline_run\generated\prompts.jsonl --images-dir out\pipeline_run\generated\images --config image_render\config.yaml"
Write-Host "Then: python db_import\import_products.py --catalog out\pipeline_run\generated\catalog.jsonl --images-dir out\pipeline_run\generated\images --skip-if-name-exists"
