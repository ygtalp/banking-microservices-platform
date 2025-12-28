# Banking Microservices Platform - Deployment Script
# PowerShell script to deploy all services with Docker

Write-Host "====================================" -ForegroundColor Cyan
Write-Host "BANKING MICROSERVICES - DEPLOYMENT" -ForegroundColor Cyan
Write-Host "====================================" -ForegroundColor Cyan
Write-Host ""

# Check if Docker is running
Write-Host "[1/6] Checking Docker status..." -ForegroundColor Yellow
try {
    $dockerVersion = docker version
    Write-Host "Success: Docker is running" -ForegroundColor Green
} catch {
    Write-Host "Error: Docker is not running! Please start Docker Desktop." -ForegroundColor Red
    exit 1
}

Write-Host ""

# Stop and remove existing containers
Write-Host "[2/6] Stopping existing containers..." -ForegroundColor Yellow
docker-compose down -v
Write-Host "Success: Containers stopped and volumes removed" -ForegroundColor Green
Write-Host ""

# Build Docker images
Write-Host "[3/6] Building Docker images..." -ForegroundColor Yellow
Write-Host "This may take several minutes..." -ForegroundColor Gray
docker-compose build --no-cache
if ($LASTEXITCODE -ne 0) {
    Write-Host "Error: Docker build failed!" -ForegroundColor Red
    exit 1
}
Write-Host "Success: Docker images built successfully" -ForegroundColor Green
Write-Host ""

# Start all services
Write-Host "[4/6] Starting all services..." -ForegroundColor Yellow
docker-compose up -d
if ($LASTEXITCODE -ne 0) {
    Write-Host "Error: Failed to start services!" -ForegroundColor Red
    exit 1
}
Write-Host "Success: All services started" -ForegroundColor Green
Write-Host ""

# Wait for services to be healthy
Write-Host "[5/6] Waiting for services to be healthy..." -ForegroundColor Yellow
Write-Host "This may take 60-90 seconds..." -ForegroundColor Gray

$maxWaitTime = 120
$startTime = Get-Date

$services = @(
    @{Name="PostgreSQL"; Container="banking-postgres"; Port=5432},
    @{Name="Redis"; Container="banking-redis"; Port=6379},
    @{Name="Kafka"; Container="banking-kafka"; Port=9092},
    @{Name="Eureka Server"; Container="banking-eureka"; Port=8761},
    @{Name="API Gateway"; Container="banking-api-gateway"; Port=8080},
    @{Name="Account Service"; Container="banking-account-service"; Port=8081},
    @{Name="Transfer Service"; Container="banking-transfer-service"; Port=8082},
    @{Name="Customer Service"; Container="banking-customer-service"; Port=8083},
    @{Name="Auth Service"; Container="banking-auth-service"; Port=8084}
)

Start-Sleep -Seconds 10

foreach ($service in $services) {
    $waited = 0
    $isHealthy = $false
    
    Write-Host "  Checking $($service.Name)..." -ForegroundColor Gray -NoNewline
    
    while ($waited -lt $maxWaitTime -and -not $isHealthy) {
        try {
            $containerStatus = docker inspect -f '{{.State.Status}}' $service.Container 2>$null
            if ($containerStatus -eq "running") {
                $isHealthy = $true
                Write-Host " Success" -ForegroundColor Green
                break
            }
        } catch {
            # Container not ready yet
        }
        
        Start-Sleep -Seconds 3
        $waited += 3
    }
    
    if (-not $isHealthy) {
        Write-Host " TIMEOUT" -ForegroundColor Red
        Write-Host ""
        Write-Host "Service failed to start. Check logs with:" -ForegroundColor Yellow
        Write-Host "  docker logs $($service.Container)" -ForegroundColor Cyan
        exit 1
    }
}

Write-Host ""

# Display service status
Write-Host "[6/6] Service Status:" -ForegroundColor Yellow
Write-Host ""
Write-Host "Infrastructure:" -ForegroundColor Cyan
Write-Host "  Success: PostgreSQL       : http://localhost:5432" -ForegroundColor Green
Write-Host "  Success: Redis            : http://localhost:6379" -ForegroundColor Green
Write-Host "  Success: Kafka            : http://localhost:9092" -ForegroundColor Green
Write-Host "  Success: Kafka UI         : http://localhost:8090" -ForegroundColor Green
Write-Host ""
Write-Host "Microservices:" -ForegroundColor Cyan
Write-Host "  Success: Eureka Server    : http://localhost:8761" -ForegroundColor Green
Write-Host "  Success: API Gateway      : http://localhost:8080" -ForegroundColor Green
Write-Host "  Success: Account Service  : http://localhost:8081" -ForegroundColor Green
Write-Host "  Success: Transfer Service : http://localhost:8082" -ForegroundColor Green
Write-Host "  Success: Customer Service : http://localhost:8083" -ForegroundColor Green
Write-Host "  Success: Auth Service     : http://localhost:8084" -ForegroundColor Green
Write-Host ""

Write-Host "====================================" -ForegroundColor Green
Write-Host "DEPLOYMENT SUCCESSFUL!" -ForegroundColor Green
Write-Host "====================================" -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Cyan
Write-Host "  1. Check Eureka Dashboard: http://localhost:8761" -ForegroundColor White
Write-Host "  2. Check Kafka UI: http://localhost:8090" -ForegroundColor White
Write-Host "  3. Run './test-services.ps1' to test APIs" -ForegroundColor White
Write-Host ""
Write-Host "To view logs:" -ForegroundColor Cyan
Write-Host "  docker-compose logs -f [service-name]" -ForegroundColor White
Write-Host ""
Write-Host "To stop all services:" -ForegroundColor Cyan
Write-Host "  docker-compose down" -ForegroundColor White
Write-Host ""
