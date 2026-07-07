# Production readiness quick check (Windows PowerShell)
# Run from repo root: powershell -File docs/deploy/verify-production-readiness.ps1

$ErrorActionPreference = 'Continue'
$passed = 0
$failed = 0
$warn = 0

function Pass($msg) { Write-Host "[PASS] $msg" -ForegroundColor Green; $script:passed++ }
function Fail($msg) { Write-Host "[FAIL] $msg" -ForegroundColor Red; $script:failed++ }
function Warn($msg) { Write-Host "[WARN] $msg" -ForegroundColor Yellow; $script:warn++ }

Write-Host ""
Write-Host "=== Laby production readiness ==="
Write-Host ""

$appYaml = Join-Path $PSScriptRoot '..\..\laby-server\src\main\resources\application.yaml'
if (Test-Path $appYaml) {
    $content = Get-Content $appYaml -Raw
    if ($content -match 'session-store:\s*redis') { Pass 'application.yaml session-store=redis' }
    else { Fail 'application.yaml missing session-store: redis' }
    if ($content -match 'max-file-size:\s*30MB') { Pass 'application.yaml upload 30MB' }
    else { Warn 'application.yaml multipart not 30MB' }
    if ($content -match 'max-concurrent-per-tenant:\s*5') { Pass 'application.yaml tenant audit concurrency' }
    else { Warn 'application.yaml missing max-concurrent-per-tenant' }
} else {
    Fail "Missing $appYaml"
}

function Test-RedisPing {
    $cli = Get-Command redis-cli -ErrorAction SilentlyContinue
    if ($cli) {
        $out = & redis-cli -h 127.0.0.1 -p 6379 ping 2>&1
        return ($out -match 'PONG')
    }
    $docker = Get-Command docker -ErrorAction SilentlyContinue
    if ($docker) {
        $names = docker ps --format '{{.Names}}' 2>$null
        if ($names -match 'laby-redis') {
            $out = docker exec laby-redis redis-cli ping 2>&1
            return ($out -match 'PONG')
        }
    }
    return $null
}

$redisOk = Test-RedisPing
if ($redisOk -eq $true) { Pass 'Redis PING ok' }
elseif ($redisOk -eq $false) { Fail 'Redis not responding' }
else { Warn 'Skip Redis check (no redis-cli / laby-redis)' }

if ($redisOk -eq $true) {
    $cli = Get-Command redis-cli -ErrorAction SilentlyContinue
    if ($cli) {
        $streamInfo = & redis-cli XINFO STREAM LegalContractAuditMessage 2>&1
    } else {
        $streamInfo = docker exec laby-redis redis-cli XINFO STREAM LegalContractAuditMessage 2>&1
    }
    if ($streamInfo -match 'length') { Pass 'Redis Stream LegalContractAuditMessage exists' }
    else { Warn 'Stream not created yet (normal before first audit)' }
}

$apiUrl = if ($env:LABY_API_URL) { $env:LABY_API_URL } else { 'http://127.0.0.1:48080/admin-api' }
try {
    $resp = Invoke-WebRequest -Uri "$apiUrl/actuator/health" -UseBasicParsing -TimeoutSec 5 -ErrorAction Stop
    if ($resp.StatusCode -eq 200) { Pass "API health $apiUrl/actuator/health" }
    else { Warn "API status $($resp.StatusCode)" }
} catch {
    Warn "API not up or actuator disabled: $apiUrl"
}

$dockerfile = Join-Path $PSScriptRoot '..\..\laby-server\Dockerfile'
if ((Get-Content $dockerfile -Raw) -match 'Xmx2g') { Pass 'Dockerfile JVM 2GB' }
else { Warn 'Dockerfile heap not 2GB' }

Write-Host ""
Write-Host "--- Summary: PASS=$passed FAIL=$failed WARN=$warn ---"
Write-Host ""
if ($failed -gt 0) { exit 1 }
exit 0
