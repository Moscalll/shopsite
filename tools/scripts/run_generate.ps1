$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot\..
python .\product_pipeline\generate.py --in .\samples\product_spec.sample.jsonl --out-dir .\out\sample_run
Write-Host "Done. See .\out\sample_run\prompts.jsonl and .\out\sample_run\catalog.jsonl"
