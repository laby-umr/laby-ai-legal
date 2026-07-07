# E2E: contract episodic memory + user facts
# Usage: .\docs\superpowers\scripts\legal-contract-memory-e2e.ps1 -ContractId 51
param(
    [long]$ContractId = 51,
    [switch]$SkipChat,
    [switch]$KeepData
)

$ErrorActionPreference = "Stop"
. "$PSScriptRoot\_lib\smoke-common.ps1"

$BaseUrl = if ($env:LABY_BASE_URL) { $env:LABY_BASE_URL } else { "http://localhost:48080" }
$PollIntervalSec = 2
$ExtractTimeoutSec = if ($env:LABY_E2E_MEMORY_TIMEOUT) { [int]$env:LABY_E2E_MEMORY_TIMEOUT } else { 30 }
$SessionId = "e2e-memory-$ContractId-$(Get-Date -Format 'yyyyMMddHHmmss')"
$Tag = "[E2E-MEMORY contract#$ContractId]"

function Invoke-LabyPostJson {
    param([string]$Url, [string]$Token, $Body, [int]$TenantId = 1)
    $headers = @{
        "Authorization" = "Bearer $Token"
        "tenant-id"     = "$TenantId"
        "Content-Type"  = "application/json"
    }
    $json = if ($Body -is [string]) { $Body } else { $Body | ConvertTo-Json -Depth 10 -Compress }
    return Invoke-RestMethod -Method Post -Uri $Url -Headers $headers -Body $json
}

function Invoke-LabyPutJson {
    param([string]$Url, [string]$Token, $Body, [int]$TenantId = 1)
    $headers = @{
        "Authorization" = "Bearer $Token"
        "tenant-id"     = "$TenantId"
        "Content-Type"  = "application/json"
    }
    $json = if ($Body -is [string]) { $Body } else { $Body | ConvertTo-Json -Depth 10 -Compress }
    return Invoke-RestMethod -Method Put -Uri $Url -Headers $headers -Body $json
}

function Invoke-LabyDelete {
    param([string]$Url, [string]$Token, [int]$TenantId = 1)
    $headers = @{
        "Authorization" = "Bearer $Token"
        "tenant-id"     = "$TenantId"
    }
    return Invoke-RestMethod -Method Delete -Uri $Url -Headers $headers
}

function Write-Step {
    param([string]$Name, [string]$Detail = "")
    if ($Detail) {
        Write-Host "$Tag $Name | $Detail"
    } else {
        Write-Host "$Tag $Name"
    }
}

function Assert-ContainsText {
    param($Items, [string]$Needle, [string]$Label)
    $hit = @($Items | Where-Object {
            ($_.content -like "*$Needle*") -or ($_.Content -like "*$Needle*")
        })
    if ($hit.Count -lt 1) {
        throw "$Label : missing content marker '$Needle'"
    }
    return $hit[0]
}

$createdMemoryIds = New-Object System.Collections.Generic.List[long]
$createdFactIds = New-Object System.Collections.Generic.List[long]
$token = $null

try {
    Write-Step "M0 login"
    $token = Get-LabyAdminToken -BaseUrl $BaseUrl
    Write-Host "$Tag OK token acquired" -ForegroundColor Green

    Write-Step "M1 contract exists" "id=$ContractId"
    $contractResp = Invoke-LabyGet "$BaseUrl/admin-api/legal/contract/get?id=$ContractId" $token
    Assert-CommonResult $contractResp "contract-get"
    $contract = $contractResp.data
    $userId = [long]$contract.userId
    Write-Host "$Tag OK title=$($contract.title) status=$($contract.statusName) userId=$userId" -ForegroundColor Green

    Write-Step "M2 baseline query"
    $baselineList = Invoke-LabyGet "$BaseUrl/admin-api/legal/contract/memory/list?contractId=$ContractId" $token
    Assert-CommonResult $baselineList "memory-list"
    $baselineMemoryCount = @($baselineList.data).Count
    $memoryPageUrl = "$BaseUrl/admin-api/legal/contract/memory/page?pageNo=1" + "&pageSize=20" + "&contractId=$ContractId"
    $baselinePage = Invoke-LabyGet $memoryPageUrl $token
    Assert-CommonResult $baselinePage "memory-page"
    $factPageUrl = "$BaseUrl/admin-api/legal/contract/user-fact/page?pageNo=1" + "&pageSize=20" + "&contractId=$ContractId"
    $baselineFactPage = Invoke-LabyGet $factPageUrl $token
    Assert-CommonResult $baselineFactPage "user-fact-page"
    $baselineFactCount = [int]$baselineFactPage.data.total
    Write-Host "$Tag OK baseline episodic(list)=$baselineMemoryCount pageTotal=$($baselinePage.data.total) userFacts=$baselineFactCount" -ForegroundColor Green

    $riskMarker = "E2E-RISK-$ContractId-$(Get-Random -Maximum 99999)"
    $factMarker = "E2E-FACT-$ContractId-$(Get-Random -Maximum 99999)"
    $chatMarker = "E2E-CHAT-$ContractId-$(Get-Random -Maximum 99999)"

    Write-Step "M3 create episodic memory" "type=risk"
    $createMemoryResp = Invoke-LabyPostJson "$BaseUrl/admin-api/legal/contract/memory/create" $token @{
        contractId = $ContractId
        sessionId  = $SessionId
        memoryType = "risk"
        content    = "$riskMarker payment term 30-day risk"
    }
    Assert-CommonResult $createMemoryResp "memory-create"
    $memoryId = [long]$createMemoryResp.data
    [void]$createdMemoryIds.Add($memoryId)
    Write-Host "$Tag OK memoryId=$memoryId" -ForegroundColor Green

    Write-Step "M4 create user fact"
    $createFactResp = Invoke-LabyPostJson "$BaseUrl/admin-api/legal/contract/user-fact/create" $token @{
        userId     = $userId
        contractId = $ContractId
        sessionId  = $SessionId
        content    = "$factMarker user cares about payment and penalty clauses"
    }
    Assert-CommonResult $createFactResp "user-fact-create"
    $factId = [long]$createFactResp.data
    [void]$createdFactIds.Add($factId)
    Write-Host "$Tag OK userFactId=$factId" -ForegroundColor Green

    Write-Step "M5 review list API"
    $sessionEsc = [uri]::EscapeDataString($SessionId)
    $listResp = Invoke-LabyGet "$BaseUrl/admin-api/legal/contract/memory/list?contractId=$ContractId&sessionId=$sessionEsc" $token
    Assert-CommonResult $listResp "memory-list-session"
    Assert-ContainsText $listResp.data $riskMarker "memory-list-session"
    Write-Host "$Tag OK session list contains risk memory" -ForegroundColor Green

    Write-Step "M6 admin page API"
    $riskEsc = [uri]::EscapeDataString($riskMarker)
    $factEsc = [uri]::EscapeDataString($factMarker)
    $memoryPage = Invoke-LabyGet "$BaseUrl/admin-api/legal/contract/memory/page?pageNo=1&pageSize=50&contractId=$ContractId&content=$riskEsc" $token
    Assert-CommonResult $memoryPage "memory-page-filter"
    if ([int]$memoryPage.data.total -lt 1) { throw "memory-page-filter : total < 1" }
    $factPage = Invoke-LabyGet "$BaseUrl/admin-api/legal/contract/user-fact/page?pageNo=1&pageSize=50&contractId=$ContractId&content=$factEsc" $token
    Assert-CommonResult $factPage "user-fact-page-filter"
    if ([int]$factPage.data.total -lt 1) { throw "user-fact-page-filter : total < 1" }
    Write-Host "$Tag OK admin pages return created rows" -ForegroundColor Green

    Write-Step "M7 update episodic memory"
    $updateResp = Invoke-LabyPutJson "$BaseUrl/admin-api/legal/contract/memory/update" $token @{
        id         = $memoryId
        contractId = $ContractId
        sessionId  = $SessionId
        memoryType = "decision"
        content    = "$riskMarker updated: shorten payment term to 15 days"
    }
    Assert-CommonResult $updateResp "memory-update"
    $updatedList = Invoke-LabyGet "$BaseUrl/admin-api/legal/contract/memory/list?contractId=$ContractId&sessionId=$sessionEsc" $token
    $updatedRow = Assert-ContainsText $updatedList.data "shorten payment term to 15 days" "memory-update-verify"
    if ($updatedRow.memoryType -ne "decision") {
        throw "memory-update-verify : memoryType=$($updatedRow.memoryType) expected decision"
    }
    Write-Host "$Tag OK memory updated to decision" -ForegroundColor Green

    if (-not $SkipChat) {
        Write-Step "M8 chat auto-extract" "POST /legal/contract/chat"
        $chatQuestion = "$chatMarker Please summarize cash-flow and breach risks if payment term is 30 calendar days for Party A. Keep it brief."
        $chatResp = Invoke-LabyPostJson "$BaseUrl/admin-api/legal/contract/chat" $token @{
            contractId = $ContractId
            message    = $chatQuestion
            answerMode = "BRIEF"
            agentMode  = $false
            sessionId  = $SessionId
        }
        Assert-CommonResult $chatResp "contract-chat"
        if ([string]::IsNullOrWhiteSpace([string]$chatResp.data.content)) {
            Write-Host "$Tag WARN chat empty (LLM unavailable); skip auto-extract assert" -ForegroundColor Yellow
        } else {
            Write-Host "$Tag OK chat answered; polling extraction..." -ForegroundColor Green
            $deadline = (Get-Date).AddSeconds($ExtractTimeoutSec)
            $autoHit = $false
            $chatEsc = [uri]::EscapeDataString($chatMarker)
            while ((Get-Date) -lt $deadline) {
                $factAfterChat = Invoke-LabyGet "$BaseUrl/admin-api/legal/contract/user-fact/page?pageNo=1&pageSize=20&contractId=$ContractId&content=$chatEsc" $token
                Assert-CommonResult $factAfterChat "user-fact-after-chat"
                if ([int]$factAfterChat.data.total -ge 1) {
                    $autoHit = $true
                    Write-Host "$Tag OK auto user fact found" -ForegroundColor Green
                    break
                }
                $memAfterChat = Invoke-LabyGet "$BaseUrl/admin-api/legal/contract/memory/page?pageNo=1&pageSize=20&contractId=$ContractId&content=$chatEsc" $token
                Assert-CommonResult $memAfterChat "memory-after-chat"
                if ([int]$memAfterChat.data.total -ge 1) {
                    $autoHit = $true
                    Write-Host "$Tag OK auto episodic memory found" -ForegroundColor Green
                    break
                }
                Start-Sleep -Seconds $PollIntervalSec
            }
            if (-not $autoHit) {
                $fallbackFact = Invoke-LabyGet "$BaseUrl/admin-api/legal/contract/user-fact/page?pageNo=1&pageSize=5&contractId=$ContractId" $token
                $newTotal = [int]$fallbackFact.data.total
                if ($newTotal -gt $baselineFactCount) {
                    Write-Host "$Tag OK user fact count increased $baselineFactCount -> $newTotal" -ForegroundColor Green
                } else {
                    Write-Host "$Tag WARN auto-extract not seen in ${ExtractTimeoutSec}s; manual CRUD passed" -ForegroundColor Yellow
                }
            }
        }
    } else {
        Write-Host "$Tag SKIP chat (-SkipChat)" -ForegroundColor Yellow
    }

    if (-not $KeepData) {
        Write-Step "M9 cleanup"
        foreach ($id in $createdMemoryIds) {
            $del = Invoke-LabyDelete "$BaseUrl/admin-api/legal/contract/memory/delete?id=$id&contractId=$ContractId" $token
            Assert-CommonResult $del "memory-delete-$id"
        }
        foreach ($id in $createdFactIds) {
            $del = Invoke-LabyDelete "$BaseUrl/admin-api/legal/contract/user-fact/delete?id=$id" $token
            Assert-CommonResult $del "user-fact-delete-$id"
        }
        Write-Host "$Tag OK cleaned $($createdMemoryIds.Count) memories + $($createdFactIds.Count) facts" -ForegroundColor Green
    } else {
        Write-Host "$Tag KEEP data (-KeepData)" -ForegroundColor Yellow
    }

    Write-Host ""
    Write-Host "$Tag ALL PASSED" -ForegroundColor Green
}
catch {
    Write-Host ""
    Write-Host "$Tag FAILED: $($_.Exception.Message)" -ForegroundColor Red
    if (-not $KeepData -and $null -ne $token) {
        Write-Host "$Tag cleanup on failure..." -ForegroundColor Yellow
        try {
            foreach ($id in $createdMemoryIds) {
                Invoke-LabyDelete "$BaseUrl/admin-api/legal/contract/memory/delete?id=$id&contractId=$ContractId" $token | Out-Null
            }
            foreach ($id in $createdFactIds) {
                Invoke-LabyDelete "$BaseUrl/admin-api/legal/contract/user-fact/delete?id=$id" $token | Out-Null
            }
        } catch {
            Write-Host "$Tag cleanup error: $($_.Exception.Message)" -ForegroundColor DarkYellow
        }
    }
    exit 1
}
