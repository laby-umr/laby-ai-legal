# 冒烟模板 — 复制为 {feature}-smoke.ps1
$ErrorActionPreference = "Stop"
. "$PSScriptRoot\_lib\smoke-common.ps1"

$BaseUrl = if ($env:LABY_BASE_URL) { $env:LABY_BASE_URL } else { "http://localhost:48080" }
$Token = Get-LabyAdminToken -BaseUrl $BaseUrl -TenantId 1

# TODO: 替换为本需求的 API
Assert-CommonResult (Invoke-LabyGet "$BaseUrl/admin-api/system/user/profile/get" $Token) "profile"

Write-Host "SMOKE OK: template"
exit 0
