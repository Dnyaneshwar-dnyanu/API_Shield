<#
.SYNOPSIS
    APIShield - Automated End-to-End System Integration Runner
.DESCRIPTION
    Executes the 10-step platform lifecycle test suite:
    - Actuator Health check across Eureka and all 8 Microservices
    - API Owner & Developer Registration & JWT Token Issuance
    - API Creation, Tiered Pricing Plans (Free/Pro), Publishing
    - Marketplace Catalog Discovery & Subscription Creation
    - Cryptographic API Key Generation (ash_live_...) & Redis caching
    - Edge Gateway 9-Phase Proxy Routing
    - Rate Limiting Token Bucket (HTTP 429) Enforcement
    - Kafka Event Propagation & Notifications
    - Telemetry Ingestion & Real-Time Analytics
    - Throughput and P50/P90/P95/P99 Latency Benchmarking
.PARAMETER Mode
    'mock' (standalone simulation contract test) or 'live' (hits running microservices)
#>

param(
    [ValidateSet('mock', 'live')]
    [string]$Mode = 'mock',
    [string]$GatewayUrl = 'http://localhost:8080'
)

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "🛡️  APIShield Automated End-to-End Verification" -ForegroundColor Cyan
Write-Host "   Mode:        $Mode" -ForegroundColor Yellow
Write-Host "   Gateway URL: $GatewayUrl" -ForegroundColor Yellow
Write-Host "============================================================" -ForegroundColor Cyan

$env:GATEWAY_URL = $GatewayUrl

if ($Mode -eq 'mock') {
    Write-Host "`nRunning End-to-End verification in Standalone Contract Mode..." -ForegroundColor Green
    node "$PSScriptRoot/e2e-runner.js" --mock
} else {
    Write-Host "`nChecking if mock upstream target server is needed..." -ForegroundColor Green
    $mockProcess = Start-Process node -ArgumentList "$PSScriptRoot/mock-target-server.js" -PassThru -NoNewWindow
    Start-Sleep -Seconds 1

    try {
        node "$PSScriptRoot/e2e-runner.js"
    } finally {
        if ($mockProcess -and !$mockProcess.HasExited) {
            Stop-Process -Id $mockProcess.Id -Force -ErrorAction SilentlyContinue
        }
    }
}

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n✅ APIShield Platform End-to-End Verification PASSED!" -ForegroundColor Green
} else {
    Write-Host "`n❌ APIShield Platform End-to-End Verification FAILED!" -ForegroundColor Red
}
exit $LASTEXITCODE
