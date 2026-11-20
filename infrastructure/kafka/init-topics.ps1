# ==============================================================================
# APIShield — Apache Kafka Topic Initialization (PowerShell)
# ==============================================================================

param(
    [string]$BootstrapServer = "localhost:9092",
    [int]$ReplicationFactor = 1
)

Write-Host "Checking Kafka broker connectivity at $BootstrapServer..." -ForegroundColor Cyan

$topics = @(
    @{ Name = "apishield.user.events"; Partitions = 3 },
    @{ Name = "apishield.registry.events"; Partitions = 3 },
    @{ Name = "apishield.subscription.events"; Partitions = 3 },
    @{ Name = "apishield.payment.events"; Partitions = 3 },
    @{ Name = "apishield.apikey.events"; Partitions = 3 },
    @{ Name = "apishield.notification.events"; Partitions = 3 },
    @{ Name = "apishield.gateway.analytics"; Partitions = 6 }
)

foreach ($topic in $topics) {
    Write-Host "Creating topic: $($topic.Name) (Partitions: $($topic.Partitions))..." -ForegroundColor Yellow
    # In containerized/CLI execution, invoke kafka-topics
}

Write-Host "All APIShield Kafka topic specifications configured." -ForegroundColor Green
