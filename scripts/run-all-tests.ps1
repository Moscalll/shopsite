<#
.SYNOPSIS
  ShopSite one-click test runner: Maven + API smoke + Playwright E2E + optional perf.

.PARAMETER BaseUrl
  Target site URL (default http://localhost:8080)

.PARAMETER SkipMaven
  Skip mvnw test

.PARAMETER SkipApi
  Skip REST API smoke tests

.PARAMETER SkipFunctional
  Skip Playwright functional tests

.PARAMETER IncludePerf
  Also run scripts/perf/collect-perf-metrics.mjs

.PARAMETER EnableOrderTest
  Run POST /api/orders in API tests (consumes stock)

.PARAMETER NoInstall
  Skip npm install / playwright install
#>
[CmdletBinding()]
param(
    [string]$BaseUrl = "http://localhost:8080",
    [switch]$SkipMaven,
    [switch]$SkipApi,
    [switch]$SkipFunctional,
    [switch]$IncludePerf,
    [switch]$EnableOrderTest,
    [switch]$NoInstall
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

$env:BASE_URL = $BaseUrl
$env:OUT_DIR = "test-results"
$env:SPRING_PROFILES_ACTIVE = $env:SPRING_PROFILES_ACTIVE
if (-not $env:SPRING_PROFILES_ACTIVE) {
    $env:SPRING_PROFILES_ACTIVE = "dev,init-data"
}
if ($EnableOrderTest) { $env:ENABLE_ORDER_TEST = "1" }

function Write-Step {
    param([string]$Msg)
    Write-Host ""
    Write-Host "== $Msg ==" -ForegroundColor Cyan
}

function Test-ServerUp {
    try {
        $null = Invoke-WebRequest -Uri $BaseUrl -UseBasicParsing -MaximumRedirection 0 -TimeoutSec 5 -ErrorAction Stop
        return $true
    } catch {
        if ($_.Exception.Response -and [int]$_.Exception.Response.StatusCode -ge 200) { return $true }
        return $false
    }
}

function Ensure-NodeDeps {
    if ($NoInstall) { return }
    if (-not (Test-Path "node_modules")) {
        Write-Host "npm install ..."
        npm install --no-fund --no-audit
    }
    $pw = Join-Path (Join-Path "node_modules" ".cache") "ms-playwright"
    if (-not (Test-Path $pw)) {
        Write-Host "playwright install chromium ..."
        npx playwright install chromium
    }
}

$failed = @()

Write-Step "0 - check server $BaseUrl"
if (-not (Test-ServerUp)) {
    Write-Host "Server not reachable. Start the app first, e.g.:" -ForegroundColor Yellow
    Write-Host '  $env:DB_USERNAME="shopsite_user"; $env:DB_PASSWORD="761943"'
    Write-Host '  .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"'
    exit 1
}
Write-Host "Server is up" -ForegroundColor Green

Ensure-NodeDeps

if (-not $SkipMaven) {
    Write-Step "1 - Maven unit tests"
    & .\mvnw.cmd test -q
    if ($LASTEXITCODE -ne 0) {
        $failed += "maven"
        Write-Host "Maven tests failed (exit $LASTEXITCODE)" -ForegroundColor Red
    } else {
        Write-Host "Maven tests passed" -ForegroundColor Green
    }
} else {
    Write-Host "Skipped Maven (-SkipMaven)"
}

if (-not $SkipApi) {
    Write-Step "2 - API smoke tests"
    node scripts/functional/run-api-smoke-tests.mjs
    if ($LASTEXITCODE -ne 0) { $failed += "api" }
} else {
    Write-Host "Skipped API (-SkipApi)"
}

if (-not $SkipFunctional) {
    Write-Step "3 - Playwright functional tests"
    node scripts/functional/run-functional-tests.mjs
    if ($LASTEXITCODE -ne 0) { $failed += "functional" }
} else {
    Write-Host "Skipped functional (-SkipFunctional)"
}

if ($IncludePerf) {
    Write-Step "4 - performance collect"
    $env:VERSION = "ci-perf"
    $env:RUNS = "3"
    $env:TARGETS = '/,/products,/product/1'
    npm run perf:collect
    if ($LASTEXITCODE -ne 0) { $failed += "perf" }
} else {
    Write-Host "Perf skipped (use -IncludePerf to enable)"
}

Write-Step "summary"
if ($failed.Count -eq 0) {
    Write-Host "All stages passed. Reports: $Root\test-results\" -ForegroundColor Green
    exit 0
}

Write-Host "Failed stages: $($failed -join ', ')" -ForegroundColor Red
Write-Host "See JSON reports under test-results\"
exit 1
