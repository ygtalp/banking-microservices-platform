# Banking Microservices - Quick Fix and Rebuild
# Fixes the @PathVariable parameter name issue and rebuilds Account Service

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "ACCOUNT SERVICE - FIX & REBUILD" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

Write-Host "`n[1/5] Backing up original file..." -ForegroundColor Yellow
Copy-Item "account-service\src\main\java\com\banking\account\controller\AccountController.java" `
          "account-service\src\main\java\com\banking\account\controller\AccountController.java.backup"
Write-Host "Backup created: AccountController.java.backup" -ForegroundColor Green

Write-Host "`n[2/5] Applying fix..." -ForegroundColor Yellow
Copy-Item "AccountController.java" `
          "account-service\src\main\java\com\banking\account\controller\AccountController.java" -Force
Write-Host "Fix applied successfully" -ForegroundColor Green

Write-Host "`n[3/5] Rebuilding Account Service..." -ForegroundColor Yellow
Set-Location account-service
mvn clean package -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed!" -ForegroundColor Red
    Set-Location ..
    exit 1
}
Set-Location ..
Write-Host "Build successful" -ForegroundColor Green

Write-Host "`n[4/5] Rebuilding Docker image..." -ForegroundColor Yellow
docker-compose build account-service --no-cache
if ($LASTEXITCODE -ne 0) {
    Write-Host "Docker build failed!" -ForegroundColor Red
    exit 1
}
Write-Host "Docker image built successfully" -ForegroundColor Green

Write-Host "`n[5/5] Restarting Account Service..." -ForegroundColor Yellow
docker-compose stop account-service
docker-compose up -d account-service
Write-Host "Waiting for service to start..." -ForegroundColor Gray
Start-Sleep -Seconds 30

Write-Host "`n========================================" -ForegroundColor Green
Write-Host "FIX APPLIED SUCCESSFULLY!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green

Write-Host "`nTesting the fix..." -ForegroundColor Cyan
Start-Sleep -Seconds 5

try {
    $response = Invoke-RestMethod -Uri "http://localhost:8081/api/v1/accounts/number/TR850000146625818780278023" -Method GET
    Write-Host "`nSUCCESS! Account endpoint is working!" -ForegroundColor Green
    Write-Host "Account Number: $($response.data.accountNumber)" -ForegroundColor Cyan
    Write-Host "Balance: $($response.data.balance) $($response.data.currency)" -ForegroundColor Cyan
    Write-Host "Status: $($response.data.status)" -ForegroundColor Cyan
} catch {
    Write-Host "`nStill failing. Check logs:" -ForegroundColor Red
    Write-Host "docker logs banking-account-service --tail 50" -ForegroundColor Yellow
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "Next step: Run './test-services-fixed.ps1' to test full system" -ForegroundColor White
Write-Host "========================================" -ForegroundColor Cyan
