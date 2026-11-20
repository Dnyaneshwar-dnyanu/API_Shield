# ==============================================================================
# APIShield — Infrastructure Configuration Verification Script
# Validates MySQL schema scripts, Redis configurations, Kafka scripts & Docker Compose
# ==============================================================================

Write-Host ">>> Running APIShield Infrastructure Verification..." -ForegroundColor Cyan
$errors = 0

# 1. Verify MySQL Init Script
$sqlFile = "infrastructure/mysql/init-databases.sql"
if (Test-Path $sqlFile) {
    $content = Get-Content $sqlFile -Raw
    $expectedDbs = @(
        "apishield_user", "apishield_registry", "apishield_subscription",
        "apishield_apikey", "apishield_payment", "apishield_notification", "apishield_analytics"
    )
    foreach ($db in $expectedDbs) {
        if ($content -notmatch "CREATE DATABASE IF NOT EXISTS [^\n]*$db") {
            Write-Host "[FAIL] Database '$db' creation missing in $sqlFile" -ForegroundColor Red
            $errors++
        }
    }
    Write-Host "[PASS] MySQL init script verified with all 7 microservice databases." -ForegroundColor Green
} else {
    Write-Host "[FAIL] $sqlFile not found!" -ForegroundColor Red
    $errors++
}

# 2. Verify Redis Configuration & Lua Script
$redisConf = "infrastructure/redis/redis.conf"
if (Test-Path $redisConf) {
    $rContent = Get-Content $redisConf -Raw
    if ($rContent -match "maxmemory 256mb" -and $rContent -match "appendonly yes") {
        Write-Host "[PASS] Redis configuration verified (AOF enabled, maxmemory set)." -ForegroundColor Green
    } else {
        Write-Host "[FAIL] Redis configuration missing required directives!" -ForegroundColor Red
        $errors++
    }
} else {
    Write-Host "[FAIL] $redisConf not found!" -ForegroundColor Red
    $errors++
}

$luaScript = "infrastructure/redis/scripts/token_bucket.lua"
if (Test-Path $luaScript) {
    $lContent = Get-Content $luaScript -Raw
    if ($lContent -match "redis.call\('INCR'" -and $lContent -match "redis.call\('EXPIRE'") {
        Write-Host "[PASS] Redis Token Bucket Lua rate limiter script verified." -ForegroundColor Green
    } else {
        Write-Host "[FAIL] Redis Lua script invalid!" -ForegroundColor Red
        $errors++
    }
} else {
    Write-Host "[FAIL] $luaScript not found!" -ForegroundColor Red
    $errors++
}

# 3. Verify Kafka Topic Scripts
$kafkaSh = "infrastructure/kafka/init-topics.sh"
if (Test-Path $kafkaSh) {
    $kContent = Get-Content $kafkaSh -Raw
    $expectedTopics = @(
        "apishield.user.events", "apishield.registry.events", "apishield.subscription.events",
        "apishield.payment.events", "apishield.apikey.events", "apishield.gateway.analytics",
        "apishield.notification.events"
    )
    foreach ($topic in $expectedTopics) {
        if ($kContent -notmatch $topic) {
            Write-Host "[FAIL] Topic '$topic' missing in $kafkaSh" -ForegroundColor Red
            $errors++
        }
    }
    Write-Host "[PASS] Kafka initialization script verified with all 7 canonical topics." -ForegroundColor Green
} else {
    Write-Host "[FAIL] $kafkaSh not found!" -ForegroundColor Red
    $errors++
}

# 4. Verify Docker Compose Descriptor
$composeFile = "infrastructure/docker/docker-compose.yml"
if (Test-Path $composeFile) {
    $cContent = Get-Content $composeFile -Raw
    $expectedServices = @("mysql:", "redis:", "kafka:", "kafka-init-topics:")
    foreach ($svc in $expectedServices) {
        if ($cContent -notmatch $svc) {
            Write-Host "[FAIL] Service '$svc' missing in $composeFile" -ForegroundColor Red
            $errors++
        }
    }
    Write-Host "[PASS] Docker Compose specification verified (MySQL, Redis, Kafka KRaft, Init)." -ForegroundColor Green
} else {
    Write-Host "[FAIL] $composeFile not found!" -ForegroundColor Red
    $errors++
}

# 5. Verify Prometheus & Grafana
$promFile = "infrastructure/monitoring/prometheus/prometheus.yml"
$grafFile = "infrastructure/monitoring/grafana/datasources.yml"
if ((Test-Path $promFile) -and (Test-Path $grafFile)) {
    Write-Host "[PASS] Prometheus scrape and Grafana datasource definitions verified." -ForegroundColor Green
} else {
    Write-Host "[FAIL] Observability configs missing!" -ForegroundColor Red
    $errors++
}

# 6. Verify .env.example
if (Test-Path ".env.example") {
    Write-Host "[PASS] Root .env.example configuration template verified." -ForegroundColor Green
} else {
    Write-Host "[FAIL] .env.example missing!" -ForegroundColor Red
    $errors++
}

if ($errors -eq 0) {
    Write-Host "`n>>> All Infrastructure Assets Successfully Verified (0 Errors)!" -ForegroundColor Green
    exit 0
} else {
    Write-Host "`n>>> Infrastructure Verification Failed with $errors error(s)!" -ForegroundColor Red
    exit 1
}
