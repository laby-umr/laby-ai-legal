# Smoke 公共函数 — laby-admin Admin API

param(
    [string]$BaseUrl = $(if ($env:LABY_BASE_URL) { $env:LABY_BASE_URL } else { "http://localhost:48080" }),
    [int]$TenantId = 1,
    [string]$Username = "admin",
    [string]$Password = "admin123"
)

function Get-LabyAdminToken {
    param(
        [string]$BaseUrl,
        [int]$TenantId = 1,
        [string]$Username = "admin",
        [string]$Password = "admin123"
    )
    $body = @{ username = $Username; password = $Password } | ConvertTo-Json
    $headers = @{ "tenant-id" = "$TenantId"; "Content-Type" = "application/json" }
    $resp = Invoke-RestMethod -Method Post -Uri "$BaseUrl/admin-api/system/auth/login" -Headers $headers -Body $body
    if ($resp.code -ne 0) { throw "Login failed: $($resp.msg)" }
    return $resp.data.accessToken
}

function Invoke-LabyGet {
    param([string]$Url, [string]$Token, [int]$TenantId = 1)
    $headers = @{
        "Authorization" = "Bearer $Token"
        "tenant-id"     = "$TenantId"
    }
    return Invoke-RestMethod -Method Get -Uri $Url -Headers $headers
}

function Assert-CommonResult {
    param($Response, [string]$Label = "API")
    if ($null -eq $Response) { throw "$Label : empty response" }
    if ($Response.code -ne 0) { throw "$Label : code=$($Response.code) msg=$($Response.msg)" }
}
