# ==============================================================================
# APIShield Platform — Unified One-Command Shutdown Script
# ==============================================================================
param (
    [switch]$StopDocker = $false
)

$ErrorActionPreference = "SilentlyContinue"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Host ""
Write-Host "========================================================================" -ForegroundColor Yellow
Write-Host "                APIShield Platform Shutdown                             " -ForegroundColor Yellow
Write-Host "========================================================================" -ForegroundColor Yellow
Write-Host ""

$appPorts = @(
    @{ Name = "Frontend (Vite)";          Port = 5173 },
    @{ Name = "API Gateway";              Port = 8080 },
    @{ Name = "User Service";             Port = 8081 },
    @{ Name = "API Registry Service";     Port = 8082 },
    @{ Name = "Subscription Service";     Port = 8083 },
    @{ Name = "API Key Service";          Port = 8084 },
    @{ Name = "Payment Service";          Port = 8085 },
    @{ Name = "Notification Service";     Port = 8086 },
    @{ Name = "Analytics Service";        Port = 8087 },
    @{ Name = "Eureka Discovery Server";  Port = 8761 }
)

Write-Host "Stopping all running microservices and frontend..." -ForegroundColor Yellow
$stoppedCount = 0

foreach ($entry in $appPorts) {
    $conn = Get-NetTCPConnection -LocalPort $entry.Port -State Listen -ErrorAction SilentlyContinue
    if ($conn) {
        $pids = $conn.OwningProcess | Select-Object -Unique
        foreach ($procId in $pids) {
            try {
                Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
                Write-Host "  - Stopped $($entry.Name) on port $($entry.Port) [PID $procId]" -ForegroundColor Green
                $stoppedCount++
            } catch {}
        }
    }
}

if ($stoppedCount -eq 0) {
    Write-Host "  No running APIShield services were detected on application ports." -ForegroundColor DarkGray
} else {
    Write-Host "`nSuccessfully stopped $stoppedCount service process(es)." -ForegroundColor Green
}

if ($StopDocker) {
    Write-Host "`nStopping Docker infrastructure containers..." -ForegroundColor Yellow
    $composeFile = Join-Path $ScriptDir "infrastructure\docker\docker-compose.yml"
    try {
        & docker compose -f $composeFile down 2>&1 | Out-Null
        Write-Host "  Docker containers stopped." -ForegroundColor Green
    } catch {
        Write-Host "  Could not stop Docker containers." -ForegroundColor DarkYellow
    }
}

Write-Host ""
Write-Host "APIShield Platform shutdown complete." -ForegroundColor Green
Write-Host "========================================================================" -ForegroundColor Yellow
Write-Host ""
