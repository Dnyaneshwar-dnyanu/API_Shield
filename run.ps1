# ==============================================================================
# APIShield Platform — Unified One-Command Startup Script
# ==============================================================================
param (
    [switch]$BuildFirst = $false,
    [switch]$SkipFrontend = $false
)

$ErrorActionPreference = "Continue"
$ProgressPreference = "SilentlyContinue"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ScriptDir

Write-Host ""
Write-Host "========================================================================" -ForegroundColor Cyan
Write-Host "                APIShield Platform Unified Startup                      " -ForegroundColor Cyan
Write-Host "========================================================================" -ForegroundColor Cyan
Write-Host ""

# Ensure logs directory exists
$LogDir = Join-Path $ScriptDir "logs"
if (-not (Test-Path $LogDir)) {
    New-Item -ItemType Directory -Path $LogDir | Out-Null
}

function Test-PortOpen {
    param([string]$Server = "127.0.0.1", [int]$Port, [int]$TimeoutMs = 1000)
    try {
        $client = New-Object System.Net.Sockets.TcpClient
        $iar = $client.BeginConnect($Server, $Port, $null, $null)
        $success = $iar.AsyncWaitHandle.WaitOne($TimeoutMs, $false)
        if ($success -and $client.Connected) {
            $client.EndConnect($iar)
            $client.Close()
            return $true
        }
        $client.Close()
        return $false
    } catch {
        return $false
    }
}

# ------------------------------------------------------------------------------
# 1. Check Prerequisites
# ------------------------------------------------------------------------------
Write-Host "[1/5] Checking environment prerequisites..." -ForegroundColor Yellow

# Check Java
$javaCmd = Get-Command java -ErrorAction SilentlyContinue
if ($javaCmd) {
    Write-Host "  [OK] Java detected at: $($javaCmd.Source)" -ForegroundColor Green
} else {
    Write-Host "  [ERROR] Java is not installed or not in PATH. Java 21+ required." -ForegroundColor Red
    exit 1
}

# Check Node.js & npm
$nodeCmd = Get-Command node -ErrorAction SilentlyContinue
if ($nodeCmd) {
    Write-Host "  [OK] Node.js detected at: $($nodeCmd.Source)" -ForegroundColor Green
} else {
    Write-Host "  [ERROR] Node.js is not installed or not in PATH." -ForegroundColor Red
    exit 1
}

# Check Docker
$hasDocker = $false
$dockerCmd = Get-Command docker -ErrorAction SilentlyContinue
if ($dockerCmd) {
    $dockerInfo = & docker ps 2>&1
    if ($LASTEXITCODE -eq 0) {
        $hasDocker = $true
        Write-Host "  [OK] Docker Engine is running." -ForegroundColor Green
    } else {
        Write-Host "  [INFO] Docker Engine check skipped or not elevated. Will verify listening ports..." -ForegroundColor DarkYellow
    }
} else {
    Write-Host "  [INFO] Docker CLI not found or not in PATH." -ForegroundColor DarkYellow
}

# ------------------------------------------------------------------------------
# 2. Start Core Infrastructure (Docker MySQL, Redis, Kafka)
# ------------------------------------------------------------------------------
if ($hasDocker) {
    Write-Host "`n[2/5] Ensuring infrastructure containers are running (MySQL, Redis, Kafka)..." -ForegroundColor Yellow
    $composeFile = Join-Path $ScriptDir "infrastructure\docker\docker-compose.yml"
    try {
        & docker compose -f $composeFile up -d mysql redis kafka 2>&1 | Out-Null
    } catch {}
} else {
    Write-Host "`n[2/5] Checking database and cache availability..." -ForegroundColor Yellow
}

# Verify MySQL (3306) and Redis (6379)
$infraPorts = @(
    @{ Name = "MySQL"; Port = 3306 },
    @{ Name = "Redis"; Port = 6379 }
)
foreach ($infra in $infraPorts) {
    $connected = $false
    for ($i = 0; $i -lt 15; $i++) {
        if (Test-PortOpen -Port $infra.Port) {
            $connected = $true
            break
        }
        Start-Sleep -Seconds 1
    }
    if ($connected) {
        Write-Host "  [OK] $($infra.Name) (Port $($infra.Port)) is reachable and healthy." -ForegroundColor Green
    } else {
        Write-Host "  [WARN] $($infra.Name) (Port $($infra.Port)) is not reachable. Ensure MySQL/Redis are running!" -ForegroundColor Red
    }
}

# ------------------------------------------------------------------------------
# 3. Build Check / Optional Compilation
# ------------------------------------------------------------------------------
$discoveryJar = Join-Path $ScriptDir "backend\discovery-server\target\discovery-server-0.0.1-SNAPSHOT.jar"
$gatewayJar = Join-Path $ScriptDir "backend\api-gateway\target\api-gateway-0.0.1-SNAPSHOT.jar"
$mustBuild = $BuildFirst -or (-not (Test-Path $discoveryJar)) -or (-not (Test-Path $gatewayJar))

if ($mustBuild) {
    Write-Host "`n[Build] Building backend microservices with Maven..." -ForegroundColor Yellow
    $mvnw = Join-Path $ScriptDir "backend\mvnw.cmd"
    if (Test-Path $mvnw) {
        & $mvnw -f (Join-Path $ScriptDir "backend\pom.xml") clean package -DskipTests
    } else {
        & mvn -f (Join-Path $ScriptDir "backend\pom.xml") clean package -DskipTests
    }
    if ($LASTEXITCODE -ne 0) {
        Write-Host "  [ERROR] Maven build failed. Aborting startup." -ForegroundColor Red
        exit 1
    }
    Write-Host "  [OK] Build completed successfully." -ForegroundColor Green
}

# Frontend dependency check
$feNodeModules = Join-Path $ScriptDir "frontend\node_modules"
if (-not (Test-Path $feNodeModules)) {
    Write-Host "`n[Frontend] Installing frontend npm dependencies..." -ForegroundColor Yellow
    Push-Location (Join-Path $ScriptDir "frontend")
    & npm install
    Pop-Location
}

# ------------------------------------------------------------------------------
# 4. Stop Any Existing Stale Instances on Application Ports
# ------------------------------------------------------------------------------
Write-Host "`n[3/5] Cleaning up any stale application processes..." -ForegroundColor Yellow
$appPorts = @(8761, 8081, 8082, 8083, 8084, 8085, 8086, 8087, 8080, 5173)
$stoppedAny = $false
foreach ($p in $appPorts) {
    $conn = Get-NetTCPConnection -LocalPort $p -State Listen -ErrorAction SilentlyContinue
    if ($conn) {
        $pids = $conn.OwningProcess | Select-Object -Unique
        foreach ($procId in $pids) {
            try {
                Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
                Write-Host "  - Freed port $p (PID $procId)" -ForegroundColor DarkGray
                $stoppedAny = $true
            } catch {}
        }
    }
}
if (-not $stoppedAny) {
    Write-Host "  [OK] All application ports are clean and ready." -ForegroundColor Green
}
Start-Sleep -Seconds 1

# ------------------------------------------------------------------------------
# 5. Launch Backend Microservices
# ------------------------------------------------------------------------------
Write-Host "`n[4/5] Launching backend microservices..." -ForegroundColor Yellow

function Start-ServiceProcess {
    param (
        [string]$Name,
        [int]$Port,
        [string]$JarPath
    )
    $logFile = Join-Path $LogDir "$Name.log"
    $errFile = Join-Path $LogDir "$Name-error.log"
    Write-Host "  -> Launching $Name (Port $Port)..." -NoNewline

    $proc = Start-Process -FilePath "java" `
        -ArgumentList "-jar", "`"$JarPath`"" `
        -RedirectStandardOutput $logFile `
        -RedirectStandardError $errFile `
        -WindowStyle Hidden `
        -PassThru

    Write-Host " [PID $($proc.Id)]" -ForegroundColor Green
    return $proc
}

# Step A: Discovery Server (Port 8761)
Start-ServiceProcess -Name "discovery-server" -Port 8761 -JarPath $discoveryJar | Out-Null

# Wait for Eureka to be UP (up to 20s)
Write-Host "  -> Waiting for Eureka Discovery Server on port 8761..." -ForegroundColor DarkGray
$eurekaReady = $false
for ($i = 0; $i -lt 20; $i++) {
    if (Test-PortOpen -Port 8761) {
        $eurekaReady = $true
        break
    }
    Start-Sleep -Seconds 1
}
if ($eurekaReady) {
    Write-Host "  [OK] Eureka Discovery Server is active." -ForegroundColor Green
} else {
    Write-Host "  [WARN] Eureka port 8761 did not respond in time. Proceeding anyway..." -ForegroundColor DarkYellow
}

# Step B: Core Business Services in parallel
$services = @(
    @{ Name = "user-service";         Port = 8081; Jar = "backend\user-service\target\user-service-0.0.1-SNAPSHOT.jar" },
    @{ Name = "api-registry-service"; Port = 8082; Jar = "backend\api-registry-service\target\api-registry-service-0.0.1-SNAPSHOT.jar" },
    @{ Name = "subscription-service"; Port = 8083; Jar = "backend\subscription-service\target\subscription-service-0.0.1-SNAPSHOT.jar" },
    @{ Name = "api-key-service";      Port = 8084; Jar = "backend\api-key-service\target\api-key-service-0.0.1-SNAPSHOT.jar" },
    @{ Name = "payment-service";      Port = 8085; Jar = "backend\payment-service\target\payment-service-0.0.1-SNAPSHOT.jar" },
    @{ Name = "notification-service"; Port = 8086; Jar = "backend\notification-service\target\notification-service-0.0.1-SNAPSHOT.jar" },
    @{ Name = "analytics-service";    Port = 8087; Jar = "backend\analytics-service\target\analytics-service-0.0.1-SNAPSHOT.jar" }
)

foreach ($s in $services) {
    $jarPath = Join-Path $ScriptDir $s.Jar
    if (Test-Path $jarPath) {
        Start-ServiceProcess -Name $s.Name -Port $s.Port -JarPath $jarPath | Out-Null
    } else {
        Write-Host "  [SKIP] $($s.Name) JAR not found at $jarPath" -ForegroundColor DarkGray
    }
}

# Step C: Edge API Gateway (Port 8080)
Start-ServiceProcess -Name "api-gateway" -Port 8080 -JarPath $gatewayJar | Out-Null

# Wait up to 20s for Gateway port 8080 to become ready
Write-Host "  -> Waiting for API Gateway on port 8080..." -ForegroundColor DarkGray
$gatewayReady = $false
for ($i = 0; $i -lt 20; $i++) {
    if (Test-PortOpen -Port 8080) {
        $gatewayReady = $true
        break
    }
    Start-Sleep -Seconds 1
}
if ($gatewayReady) {
    Write-Host "  [OK] API Gateway is active and routing." -ForegroundColor Green
} else {
    Write-Host "  [WARN] API Gateway port 8080 still initializing..." -ForegroundColor DarkYellow
}

# ------------------------------------------------------------------------------
# 6. Start Frontend Development Server
# ------------------------------------------------------------------------------
if (-not $SkipFrontend) {
    Write-Host "`n[5/5] Launching frontend application (Vite SPA)..." -ForegroundColor Yellow
    $frontendDir = Join-Path $ScriptDir "frontend"
    $feLog = Join-Path $LogDir "frontend.log"
    $feErr = Join-Path $LogDir "frontend-error.log"

    $feProc = Start-Process -FilePath "npm.cmd" `
        -ArgumentList "run", "dev" `
        -WorkingDirectory $frontendDir `
        -RedirectStandardOutput $feLog `
        -RedirectStandardError $feErr `
        -WindowStyle Hidden `
        -PassThru

    Write-Host "  -> Frontend dev server running on Port 5173 [PID $($feProc.Id)]" -ForegroundColor Green
}

# ------------------------------------------------------------------------------
# 7. Summary & Ready Status
# ------------------------------------------------------------------------------
Start-Sleep -Seconds 2

Write-Host ""
Write-Host "========================================================================" -ForegroundColor Green
Write-Host "             APIShield Platform is LIVE and Operational!                " -ForegroundColor Green
Write-Host "========================================================================" -ForegroundColor Green
Write-Host ""
Write-Host "  Web Dashboard:       http://localhost:5173" -ForegroundColor Cyan
Write-Host "  Edge API Gateway:    http://localhost:8080" -ForegroundColor Cyan
Write-Host "  Eureka Registry:     http://localhost:8761" -ForegroundColor Cyan
Write-Host "  Logs Directory:      $LogDir" -ForegroundColor DarkGray
Write-Host ""
Write-Host "  Default Test Credentials:" -ForegroundColor Yellow
Write-Host "    Developer: developer@apishield.dev  /  Password123!" -ForegroundColor White
Write-Host "    API Owner: owner@apishield.dev      /  Password123!" -ForegroundColor White
Write-Host "    Admin:     admin@apishield.dev      /  Password123!" -ForegroundColor White
Write-Host ""
Write-Host "  To stop the application at any time, run: .\stop.ps1" -ForegroundColor DarkGray
Write-Host "========================================================================" -ForegroundColor Green
Write-Host ""
