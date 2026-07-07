# Knowledge ingest integration test
$ErrorActionPreference = "Stop"
$baseUrl = "http://127.0.0.1:48080/admin-api"
$tenantId = "1"

function Write-Result($name, $ok, $detail) {
    $mark = if ($ok) { "[OK]" } else { "[FAIL]" }
    Write-Host "$mark $name"
    if ($detail) { Write-Host "    $detail" }
}

Write-Host "=== 1. Server ==="
try {
    Invoke-WebRequest -Uri "$baseUrl/system/auth/login" -Method OPTIONS -TimeoutSec 3 -UseBasicParsing | Out-Null
    Write-Result "port 48080" $true ""
} catch {
    if ($_.Exception.Message -match "405|400|401|403") {
        Write-Result "port 48080" $true "server reachable"
    } else {
        Write-Result "port 48080" $false $_.Exception.Message
        exit 1
    }
}

Write-Host "`n=== 2. Login ==="
$loginBody = '{"username":"admin","password":"admin123"}'
$login = Invoke-RestMethod -Uri "$baseUrl/system/auth/login" -Method POST -ContentType "application/json" -Headers @{ "tenant-id" = $tenantId } -Body $loginBody
if ($login.code -ne 0) {
    Write-Result "login" $false "code=$($login.code) msg=$($login.msg)"
    exit 1
}
$token = $login.data.accessToken
$headers = @{ Authorization = "Bearer $token"; "tenant-id" = $tenantId }
Write-Result "login" $true "token ok"

Write-Host "`n=== 3. MySQL columns ==="
try {
    $q = "SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='laby-system' AND TABLE_NAME='ai_knowledge_document' AND COLUMN_NAME LIKE 'ingest%';"
    $cols = & mysql -h127.0.0.1 -P3308 -uroot -p123456 -N -e $q 2>$null
    $colStr = ($cols | Out-String).Trim()
    $ok = $colStr -match "ingest_status" -and $colStr -match "ingest_error"
    Write-Result "ingest columns" $ok $colStr
} catch {
    Write-Result "ingest columns" $false "mysql cli skip: $($_.Exception.Message)"
}

Write-Host "`n=== 4. document/page ==="
$page = Invoke-RestMethod -Uri "$baseUrl/ai/knowledge/document/page?pageNo=1&pageSize=5" -Headers $headers
if ($page.code -ne 0) {
    Write-Result "document/page" $false $page.msg
    exit 1
}
$doc = $page.data.list | Select-Object -First 1
$names = @()
if ($doc) { $names = $doc.PSObject.Properties.Name }
$hasField = $names -contains "ingestStatus"
Write-Result "ingestStatus in page VO" $hasField $(if ($doc) { "id=$($doc.id) ingestStatus=$($doc.ingestStatus)" } else { "no documents" })

Write-Host "`n=== 5. get-process-list ==="
if (-not $doc) {
    Write-Result "get-process-list" $false "no document"
} else {
    $docId = $doc.id
    $proc = Invoke-RestMethod -Uri "$baseUrl/ai/knowledge/segment/get-process-list?documentIds=$docId" -Headers $headers
    if ($proc.code -ne 0) {
        Write-Result "get-process-list" $false $proc.msg
    } else {
        $item = $proc.data | Select-Object -First 1
        $pn = $item.PSObject.Properties.Name
        $ok = $pn -contains "ingestStatus"
        Write-Result "ingestStatus in process VO" $ok "doc=$($item.documentId) count=$($item.count) emb=$($item.embeddingCount) status=$($item.ingestStatus) err=$($item.ingestError)"
    }
}

Write-Host "`n=== 6. retry-ingest (optional) ==="
$retryDoc = $page.data.list | Where-Object { $_.ingestStatus -eq 40 -or $_.ingestStatus -eq 0 } | Select-Object -First 1
if (-not $retryDoc) {
    Write-Host "    skip: no FAILED(40) or PENDING(0) doc on page 1"
} else {
    $retry = Invoke-RestMethod -Uri "$baseUrl/ai/knowledge/document/retry-ingest?id=$($retryDoc.id)" -Method POST -Headers $headers
    Write-Result "retry-ingest" ($retry.code -eq 0) "doc=$($retryDoc.id) code=$($retry.code)"
    if ($retry.code -eq 0) {
        Start-Sleep -Seconds 2
        $proc2 = Invoke-RestMethod -Uri "$baseUrl/ai/knowledge/segment/get-process-list?documentIds=$($retryDoc.id)" -Headers $headers
        $p2 = $proc2.data[0]
        Write-Host "    after retry: ingestStatus=$($p2.ingestStatus) count=$($p2.count) embedding=$($p2.embeddingCount)"
    }
}

Write-Host "`n=== done ==="
