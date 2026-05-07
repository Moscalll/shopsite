$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot\..
if (-not (Test-Path .\.env)) {
  Write-Host "Missing tools\.env. Copy tools\.env.example -> tools\.env and fill MySQL + SHOPSITE_UPLOAD_DIR."
  exit 2
}
python .\db_import\import_products.py $args
