# 内联 FQN 自检 — 对应 laby-global §1.4
# 用法: .\docs\superpowers\scripts\check-fqn.ps1
# 退出码: 0 = 通过；1 = 存在内联 FQN（import/package/JavaDoc @link 除外）
$ErrorActionPreference = "Stop"

$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..\..")

function Test-IsFqnViolationLine {
    param([string]$Line)
    if ([string]::IsNullOrWhiteSpace($Line)) { return $false }
    if ($Line -match '^\s*(package|import)\s') { return $false }
    if ($Line -match '@(link|see)\s+(com\.|io\.agentscope\.)') { return $false }
    if ($Line -match '^\s*\*\s*@(link|see)\s') { return $false }
    # 字符串字面量中的类名（如 isNotPresent("com.laby...")）不计入违规
    $withoutStrings = $Line -replace '"[^"]*"', '' -replace "'[^']*'", ''
    if ($Line -match 'com\.laby\.' -and $withoutStrings -notmatch 'com\.laby\.') { return $false }
    if ($Line -match 'io\.agentscope\.' -and $withoutStrings -notmatch 'io\.agentscope\.') { return $false }
    return $true
}

function Search-JavaFqn {
    param(
        [string]$Pattern,
        [string[]]$SearchPaths
    )
    $violations = New-Object System.Collections.Generic.List[string]
    $useRg = $null -ne (Get-Command rg -ErrorAction SilentlyContinue)

    foreach ($rel in $SearchPaths) {
        $target = Join-Path $RepoRoot $rel
        if (-not (Test-Path $target)) {
            continue
        }
        $hits = @()
        if ($useRg) {
            $hits = & rg $Pattern --glob "*.java" $target 2>$null
        } else {
            $files = Get-ChildItem -Path $target -Filter "*.java" -Recurse -File -ErrorAction SilentlyContinue
            foreach ($file in $files) {
                $matches = Select-String -Path $file.FullName -Pattern $Pattern -AllMatches -ErrorAction SilentlyContinue
                foreach ($m in $matches) {
                    $codeLine = $m.Line.TrimEnd()
                    if (Test-IsFqnViolationLine $codeLine) {
                        $relPath = $m.Path.Substring($RepoRoot.Path.Length + 1)
                        $violations.Add("${relPath}:$($m.LineNumber):$codeLine")
                    }
                }
            }
            continue
        }
        foreach ($line in $hits) {
            $codeLine = $line
            if ($line -match '^[^:]+:\d+:(.+)$') {
                $codeLine = $Matches[1]
            }
            if (Test-IsFqnViolationLine $codeLine) {
                $violations.Add($line)
            }
        }
    }
    return $violations
}

$checks = @(
    @{
        Name = "com.laby inline FQN"
        Pattern = '[^/]\bcom\.laby\.[a-z]'
        Paths = @("laby-module-legal", "laby-module-ai", "laby-framework")
    },
    @{
        Name = "io.agentscope inline FQN"
        Pattern = '[^/]\bio\.agentscope\.[a-z]'
        Paths = @("laby-module-ai", "laby-module-legal")
    }
)

$allViolations = New-Object System.Collections.Generic.List[string]
foreach ($check in $checks) {
    $found = Search-JavaFqn -Pattern $check.Pattern -SearchPaths $check.Paths
    foreach ($v in $found) {
        $allViolations.Add("[$($check.Name)] $v")
    }
}

if ($allViolations.Count -gt 0) {
    Write-Host "FQN CHECK FAILED: $($allViolations.Count) violation(s)" -ForegroundColor Red
    $allViolations | ForEach-Object { Write-Host $_ }
    Write-Host ""
    Write-Host "Fix: use top-level import + short class name (laby-global §1.4)"
    exit 1
}

Write-Host "FQN CHECK OK: no inline FQN in scanned modules"
exit 0
