# Deploy Customer Service
# Starts the Customer Service container

$ErrorActionPreference = "Stop"

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "   Deploying Customer Service" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# Check if dependencies are running
Write-Host "[1/3] Checking dependencies..." -ForegroundColor Yellow

$requiredServices = @("banking-postgres", "banking-kafka", "banking-redis", "banking-eureka", "banking-account-service")
$missingServices = @()

foreach ($service in $requiredServices) {
    $running = docker ps --filter "name=$service" --filter "status=running" --format "{{.Names}}"
    if (-not $running) {
        $missingServices += $service
    }
}

if ($missingServices.Count -gt 0) {
    Write-Host "  ✗ Missing required services:" -ForegroundColor Red
    foreach ($service in $missingServices) {
        Write-Host "    - $service" -ForegroundColor Red
    }
    Write-Host "`nPlease start dependencies first with: docker-compose up -d" -ForegroundColor Yellow
    exit 1
}

Write-Host "  ✓ All dependencies running" -ForegroundColor Green

# Deploy Customer Service
Write-Host "`n[2/3] Starting Customer Service..." -ForegroundColor Yellow
try {
    docker-compose up -d customer-service
    Write-Host "  ✓ Container started" -ForegroundColor Green
}
catch {
    Write-Host "  ✗ Failed to start container: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Wait for health check
Write-Host "`n[3/3] Waiting for health check..." -ForegroundColor Yellow
$maxAttempts = 30
$attempt = 0
$healthy = $false

while ($attempt -lt $maxAttempts -and -not $healthy) {
    Start-Sleep -Seconds 2
    $attempt++

    try {
        $response = Invoke-RestMethod -Uri "http://localhost:8083/actuator/health" -TimeoutSec 2 -ErrorAction SilentlyContinue
        if ($response.status -eq "UP") {
            $healthy = $true
        }
    }
    catch {
        Write-Host "  Attempt $attempt/$maxAttempts - Waiting..." -ForegroundColor Gray
    }
}

if ($healthy) {
    Write-Host "  ✓ Service is healthy" -ForegroundColor Green

    # Check Eureka registration
    Start-Sleep -Seconds 5
    try {
        $eurekaResponse = Invoke-RestMethod -Uri "http://localhost:8761/eureka/apps/customer-service" -ErrorAction SilentlyContinue
        if ($eurekaResponse) {
            Write-Host "  ✓ Registered with Eureka" -ForegroundColor Green
        }
    }
    catch {
        Write-Host "  ! Eureka registration pending..." -ForegroundColor Yellow
    }
}
else {
    Write-Host "  ✗ Health check timeout" -ForegroundColor Red
    Write-Host "`nChecking logs..." -ForegroundColor Yellow
    docker logs --tail 50 banking-customer-service
    exit 1
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "   Deployment Complete" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

Write-Host "Customer Service deployed successfully! ✓" -ForegroundColor Green
Write-Host "`nService Details:" -ForegroundColor White
Write-Host "  URL: http://localhost:8083" -ForegroundColor Gray
Write-Host "  Health: http://localhost:8083/actuator/health" -ForegroundColor Gray
Write-Host "  API: http://localhost:8080/api/v1/customers" -ForegroundColor Gray
Write-Host "`nRun './scripts/test/test-customer-service.ps1' to test" -ForegroundColor Gray
