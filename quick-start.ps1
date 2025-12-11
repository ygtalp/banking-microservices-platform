# Banking Microservices Platform - Quick Start
# All-in-one script: Build + Deploy + Test

param(
    [switch]$SkipBuild,
    [switch]$SkipDeploy,
    [switch]$SkipTest,
    [switch]$Clean
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "BANKING MICROSERVICES - QUICK START" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# Show what will be executed
Write-Host ""
Write-Host "Execution Plan:" -ForegroundColor Yellow
if (-not $SkipBuild) {
    Write-Host "  [YES] Maven Build" -ForegroundColor Green
} else {
    Write-Host "  [NO] Maven Build (skipped)" -ForegroundColor Gray
}
if (-not $SkipDeploy) {
    Write-Host "  [YES] Docker Deploy" -ForegroundColor Green
} else {
    Write-Host "  [NO] Docker Deploy (skipped)" -ForegroundColor Gray
}
if (-not $SkipTest) {
    Write-Host "  [YES] API Tests" -ForegroundColor Green
} else {
    Write-Host "  [NO] API Tests (skipped)" -ForegroundColor Gray
}

Write-Host ""
$confirm = Read-Host "Continue? (Y/n)"
if ($confirm -eq "n" -or $confirm -eq "N") {
    Write-Host "Aborted." -ForegroundColor Red
    exit 0
}

$startTime = Get-Date

# Clean if requested
if ($Clean) {
    Write-Host ""
    Write-Host "[CLEAN] Removing containers and volumes..." -ForegroundColor Yellow
    docker-compose down -v
    Write-Host "Cleanup complete" -ForegroundColor Green
}

# Step 1: Build
if (-not $SkipBuild) {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "STEP 1: MAVEN BUILD" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan

    & .\scripts\build\build-fixed.ps1

    if ($LASTEXITCODE -ne 0) {
        Write-Host ""
        Write-Host "Build failed! Aborting." -ForegroundColor Red
        exit 1
    }
}

# Step 2: Deploy
if (-not $SkipDeploy) {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "STEP 2: DOCKER DEPLOY" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan

    & .\scripts\deploy\deploy-fixed.ps1

    if ($LASTEXITCODE -ne 0) {
        Write-Host ""
        Write-Host "Deployment failed! Aborting." -ForegroundColor Red
        exit 1
    }
}

# Step 3: Test
if (-not $SkipTest) {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "STEP 3: API TESTS" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan

    Write-Host "Waiting 10 seconds for services to stabilize..." -ForegroundColor Gray
    Start-Sleep -Seconds 10

    & .\scripts\test\test-services-fixed.ps1
}

# Summary
$endTime = Get-Date
$duration = $endTime - $startTime

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "QUICK START COMPLETE!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Total Time: $($duration.Minutes)m $($duration.Seconds)s" -ForegroundColor Cyan

Write-Host ""
Write-Host "Services are running at:" -ForegroundColor Cyan
Write-Host "  Eureka Dashboard: http://localhost:8761" -ForegroundColor White
Write-Host "  API Gateway:      http://localhost:8080" -ForegroundColor White
Write-Host "  Kafka UI:         http://localhost:8090" -ForegroundColor White

Write-Host ""
Write-Host "Useful commands:" -ForegroundColor Cyan
Write-Host "  View logs:     docker-compose logs -f [service]" -ForegroundColor White
Write-Host "  Stop services: docker-compose down" -ForegroundColor White
Write-Host "  Restart:       docker restart banking-[service]" -ForegroundColor White
Write-Host ""