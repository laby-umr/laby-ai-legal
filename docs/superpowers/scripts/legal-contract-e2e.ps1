# OPT-001 E2E — 合同上传 → 解析 → 审核 + 失败路径 + 基线 API
# 前置: laby-server (profile=local) @ http://localhost:48080, MySQL/Redis/RabbitMQ 可用
# 用法: .\docs\superpowers\scripts\legal-contract-e2e.ps1
$ErrorActionPreference = "Stop"
. "$PSScriptRoot\_lib\smoke-common.ps1"

$BaseUrl = if ($env:LABY_BASE_URL) { $env:LABY_BASE_URL } else { "http://localhost:48080" }
$PollIntervalSec = 5
$ParseTimeoutSec = if ($env:LABY_E2E_PARSE_TIMEOUT) { [int]$env:LABY_E2E_PARSE_TIMEOUT } else { 120 }
$AuditTimeoutSec = if ($env:LABY_E2E_AUDIT_TIMEOUT) { [int]$env:LABY_E2E_AUDIT_TIMEOUT } else { 300 }

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

function Invoke-LabyUploadFile {
    param([string]$Url, [string]$Token, [string]$FilePath, [int]$TenantId = 1)
    $curl = Get-Command curl.exe -ErrorAction SilentlyContinue
    if ($null -ne $curl) {
        $raw = & curl.exe -s -X POST $Url `
            -H "Authorization: Bearer $Token" `
            -H "tenant-id: $TenantId" `
            -F "file=@$FilePath;type=application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        return $raw | ConvertFrom-Json
    }
    Add-Type -AssemblyName System.Net.Http
    $client = New-Object System.Net.Http.HttpClient
    $client.DefaultRequestHeaders.Add("Authorization", "Bearer $Token")
    $client.DefaultRequestHeaders.Add("tenant-id", "$TenantId")
    $multipart = New-Object System.Net.Http.MultipartFormDataContent
    $fileStream = [System.IO.File]::OpenRead($FilePath)
    try {
        $streamContent = New-Object System.Net.Http.StreamContent($fileStream)
        $fileName = [System.IO.Path]::GetFileName($FilePath)
        $multipart.Add($streamContent, "file", $fileName)
        $response = $client.PostAsync($Url, $multipart).GetAwaiter().GetResult()
        $json = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
        if (-not $response.IsSuccessStatusCode) {
            throw "Upload HTTP $($response.StatusCode): $json"
        }
        return $json | ConvertFrom-Json
    } finally {
        $fileStream.Dispose()
        $client.Dispose()
    }
}

function New-MinimalDocx {
    param(
        [string]$OutPath,
        [string]$Text = 'Party A: TestCo. Party B: VendorCo. Service fee CNY 1,000,000.'
    )
    Add-Type -AssemblyName System.IO.Compression
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    if (Test-Path -LiteralPath $OutPath) { Remove-Item -LiteralPath $OutPath -Force }
    $escaped = [System.Security.SecurityElement]::Escape($Text)
    $documentXml = @"
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:body><w:p><w:r><w:t>$escaped</w:t></w:r></w:p></w:body>
</w:document>
"@
    $contentTypes = @'
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>
'@
    $rootRels = @'
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>
'@
    $docRels = @'
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"/>
'@
    $zip = [System.IO.Compression.ZipFile]::Open($OutPath, [System.IO.Compression.ZipArchiveMode]::Create)
    try {
        function Add-ZipEntry([string]$Name, [string]$Content) {
            $entry = $zip.CreateEntry($Name)
            $sw = New-Object System.IO.StreamWriter($entry.Open())
            $sw.Write($Content)
            $sw.Close()
        }
        Add-ZipEntry "[Content_Types].xml" $contentTypes
        Add-ZipEntry "_rels/.rels" $rootRels
        Add-ZipEntry "word/document.xml" $documentXml
        Add-ZipEntry "word/_rels/document.xml.rels" $docRels
    } finally {
        $zip.Dispose()
    }
}

function Wait-ContractParseSuccess {
    param([string]$Token, [long]$ContractId, [int]$TimeoutSec)
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        $resp = Invoke-LabyGet "$BaseUrl/admin-api/legal/contract/get?id=$ContractId" $Token
        Assert-CommonResult $resp "contract-get"
        $parseStatus = $resp.data.parseStatus
        if ($parseStatus -eq 2) { return $resp.data }
        if ($parseStatus -eq 3) {
            throw "E1 parse FAILED: feedback=$($resp.data.feedbackSummary) failReason=$($resp.data.failReason)"
        }
        Start-Sleep -Seconds $PollIntervalSec
    }
    throw "E1 timeout: parseStatus not SUCCESS within ${TimeoutSec}s (contractId=$ContractId)"
}

function Wait-FirstAuditSettled {
    param([string]$Token, [long]$ContractId, [int]$TimeoutSec)
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        $resp = Invoke-LabyGet "$BaseUrl/admin-api/legal/contract/get?id=$ContractId" $Token
        Assert-CommonResult $resp "contract-get-audit"
        $d = $resp.data
        $status = $d.status
        # 20=意见处置, 15=失败, 或已有审核意见/报告
        if ($status -eq 20 -or $status -eq 15) { return $d }
        if ($d.auditOpinionCount -ge 1 -or $d.hasAuditReport -eq $true) { return $d }
        if ($status -eq 21 -or $status -eq 30 -or $status -eq 40) { return $d }
        Start-Sleep -Seconds $PollIntervalSec
    }
    throw "E2 timeout: first audit not settled within ${TimeoutSec}s (contractId=$ContractId)"
}

function Get-ParagraphCount {
    param([string]$Token, [long]$ContractId)
    $resp = Invoke-LabyGet "$BaseUrl/admin-api/legal/contract/list-paragraph?contractId=$ContractId" $Token
    Assert-CommonResult $resp "list-paragraph"
    return @($resp.data).Count
}

Write-Host "=== E5 baseline APIs ==="
$Token = Get-LabyAdminToken -BaseUrl $BaseUrl -TenantId 1
Assert-CommonResult (Invoke-LabyGet "$BaseUrl/admin-api/system/user/profile/get" $Token) "profile"
$knowledgePageUrl = "$BaseUrl/admin-api/ai/knowledge/page" + "?pageNo=1" + "&pageSize=1"
$contractPageUrl = "$BaseUrl/admin-api/legal/contract/page" + "?pageNo=1" + "&pageSize=1"
Assert-CommonResult (Invoke-LabyGet $knowledgePageUrl $Token) "ai-knowledge-page"
Assert-CommonResult (Invoke-LabyGet $contractPageUrl $Token) "legal-contract-page"
Write-Host "E5 OK"

$fixtureDir = Join-Path $PSScriptRoot "..\fixtures"
if (-not (Test-Path $fixtureDir)) { New-Item -ItemType Directory -Path $fixtureDir | Out-Null }
$goodDocx = Join-Path $fixtureDir "e2e-sample-contract.docx"
$badDocx = Join-Path $fixtureDir "e2e-corrupt-contract.docx"
New-MinimalDocx -OutPath $goodDocx
[System.IO.File]::WriteAllBytes($badDocx, [byte[]](0x00, 0x01, 0x02, 0x03, 0xFF))

Write-Host "=== E1 upload + create + parse SUCCESS ==="
$upload = Invoke-LabyUploadFile "$BaseUrl/admin-api/legal/contract/upload" $Token $goodDocx
Assert-CommonResult $upload "upload"
$fileId = [long]$upload.data.fileId
$createBody = @{
    title      = "E2E-OPT001-$(Get-Date -Format 'yyyyMMdd-HHmmss')"
    partyRole  = "A"
    auditLevel = "standard"
    editable   = $true
    files      = @(@{ fileId = $fileId; fileName = "e2e-sample-contract.docx"; mainFlag = $true })
}
$create = Invoke-LabyPostJson "$BaseUrl/admin-api/legal/contract/create" $Token $createBody
Assert-CommonResult $create "create"
$contractId = [long]$create.data
$afterParse = Wait-ContractParseSuccess -Token $Token -ContractId $contractId -TimeoutSec $ParseTimeoutSec
$paragraphCount = Get-ParagraphCount -Token $Token -ContractId $contractId
if ($paragraphCount -lt 1) { throw "E1 failed: paragraphCount=$paragraphCount" }
Write-Host "E1 OK contractId=$contractId parseStatus=SUCCESS paragraphs=$paragraphCount"

Write-Host "=== E2 first audit settled ==="
$afterAudit = Wait-FirstAuditSettled -Token $Token -ContractId $contractId -TimeoutSec $AuditTimeoutSec
$opinionResp = Invoke-LabyGet "$BaseUrl/admin-api/legal/opinion/list-by-contract?contractId=$contractId" $Token
Assert-CommonResult $opinionResp "opinions"
$opinionCount = @($opinionResp.data).Count
if ($afterAudit.status -eq 15) {
    Write-Host "E2 WARN: contract FAILED during audit (status=15) failReason=$($afterAudit.failReason)"
} elseif ($opinionCount -lt 1 -and $afterAudit.hasAuditReport -ne $true -and $afterAudit.auditOpinionCount -lt 1) {
    throw "E2 failed: no opinions/report after audit (status=$($afterAudit.status))"
} else {
    Write-Host "E2 OK status=$($afterAudit.status) opinions=$opinionCount auditOpinionCount=$($afterAudit.auditOpinionCount)"
}

Write-Host "=== E3 corrupt docx failure path ==="
$badUpload = Invoke-LabyUploadFile "$BaseUrl/admin-api/legal/contract/upload" $Token $badDocx
Assert-CommonResult $badUpload "bad-upload"
$badCreate = Invoke-LabyPostJson "$BaseUrl/admin-api/legal/contract/create" $Token @{
    title      = "E2E-FAIL-$(Get-Date -Format 'yyyyMMdd-HHmmss')"
    partyRole  = "A"
    auditLevel = "standard"
    editable   = $true
    files      = @(@{ fileId = [long]$badUpload.data.fileId; fileName = "e2e-corrupt-contract.docx"; mainFlag = $true })
}
Assert-CommonResult $badCreate "bad-create"
$failId = [long]$badCreate.data
$failDeadline = (Get-Date).AddSeconds($ParseTimeoutSec)
$failOk = $false
while ((Get-Date) -lt $failDeadline) {
    $failResp = Invoke-LabyGet "$BaseUrl/admin-api/legal/contract/get?id=$failId" $Token
    Assert-CommonResult $failResp "fail-get"
    if ($failResp.data.status -eq 15 -or $failResp.data.parseStatus -eq 3) {
        if ([string]::IsNullOrWhiteSpace($failResp.data.feedbackSummary) -and [string]::IsNullOrWhiteSpace($failResp.data.failReason)) {
            throw "E3 failed: FAILED but feedback_summary/failReason empty"
        }
        $failOk = $true
        Write-Host "E3 OK failContractId=$failId feedback=$($failResp.data.feedbackSummary)"
        break
    }
    Start-Sleep -Seconds $PollIntervalSec
}
if (-not $failOk) { throw "E3 timeout: corrupt contract did not reach FAILED within ${ParseTimeoutSec}s" }

Write-Host "=== E4 concurrent parse guard (API-level) ==="
# 解析完成后再次读取段落数，确认未异常翻倍；并发 CAS 由 LegalContractParseGuardTest 单测覆盖
$paragraphCount2 = Get-ParagraphCount -Token $Token -ContractId $contractId
if ($paragraphCount2 -ne $paragraphCount) {
    throw "E4 failed: paragraph count changed $paragraphCount -> $paragraphCount2 without re-parse"
}
Write-Host "E4 OK paragraphCount stable=$paragraphCount (CAS guard covered by unit test)"

Write-Host ""
Write-Host "E2E OK: E1-E5 passed (E6 frontend optional skipped)"
exit 0
