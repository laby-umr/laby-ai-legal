# 基础冒烟 — 登录 + 法务/AI 分页可达
$ErrorActionPreference = "Stop"
. "$PSScriptRoot\_lib\smoke-common.ps1"

$BaseUrl = if ($env:LABY_BASE_URL) { $env:LABY_BASE_URL } else { "http://localhost:48080" }
$Token = Get-LabyAdminToken -BaseUrl $BaseUrl -TenantId 1

Assert-CommonResult (Invoke-LabyGet "$BaseUrl/admin-api/system/user/profile/get" $Token) "profile"
Assert-CommonResult (Invoke-LabyGet "$BaseUrl/admin-api/legal/contract/page?pageNo=1&pageSize=1" $Token) "legal-contract-page"
Assert-CommonResult (Invoke-LabyGet "$BaseUrl/admin-api/ai/knowledge/page?pageNo=1&pageSize=1" $Token) "ai-knowledge-page"

Write-Host "SMOKE OK: baseline"
exit 0
