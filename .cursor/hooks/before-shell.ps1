# Cursor beforeShellExecution — Git 安全 + FQN 门禁
# stdin: JSON { "command": "..." }
# stdout: { "permission": "allow" | "deny" | "ask", "user_message": "...", "agent_message": "..." }

$ErrorActionPreference = "Stop"

function Write-Allow {
    Write-Output '{"permission":"allow"}'
    exit 0
}

function Write-Deny {
    param([string]$Message)
    $payload = @{
        permission    = "deny"
        user_message  = $Message
        agent_message = $Message
    } | ConvertTo-Json -Compress
    Write-Output $payload
    exit 0
}

try {
    $raw = [Console]::In.ReadToEnd()
    if ([string]::IsNullOrWhiteSpace($raw)) {
        Write-Allow
    }
    $input = $raw | ConvertFrom-Json
    $cmd = [string]$input.command
    if ([string]::IsNullOrWhiteSpace($cmd)) {
        Write-Allow
    }

    # 禁止 staging sql/
    if ($cmd -match '(?i)git\s+add' -and $cmd -match '(?i)sql[/\\]') {
        Write-Deny "Blocked: do not git add sql/. SQL stays local per laby-ai-legal policy."
    }

    # 禁止 force push 到 main/master
    if ($cmd -match '(?i)git\s+push' -and $cmd -match '(?i)--force' -and $cmd -match '(?i)(main|master)') {
        Write-Deny "Blocked: force push to main/master is not allowed."
    }

    # git commit 前：检查 staged 是否含 sql/ + 跑 FQN 自检
    if ($cmd -match '(?i)git\s+commit') {
        $repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
        Push-Location $repoRoot
        try {
            $staged = & git diff --cached --name-only 2>$null
            if ($staged -match '(?i)^sql/') {
                Write-Deny "Blocked: sql/ files are staged. Run: git restore --staged sql/"
            }

            $fqnScript = Join-Path $repoRoot "docs\superpowers\scripts\check-fqn.ps1"
            if (Test-Path $fqnScript) {
                & powershell -NoProfile -ExecutionPolicy Bypass -File $fqnScript | Out-Null
                if ($LASTEXITCODE -ne 0) {
                    Write-Deny "Blocked: FQN check failed. Run docs/superpowers/scripts/check-fqn.ps1 and fix imports."
                }
            }
        }
        finally {
            Pop-Location
        }
    }

    Write-Allow
}
catch {
    # fail open — 不因 hook 异常阻断正常开发
    Write-Allow
}
