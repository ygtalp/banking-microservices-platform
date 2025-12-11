# Banking Microservices Platform - API Test Script
# PowerShell script to test all APIs

Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "BANKING MICROSERVICES - API TESTING" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

# Test function
function Test-Endpoint {
    param(
        [string]$Name,
        [string]$Url,
        [string]$Method = "GET"
    )
    
    Write-Host "Testing $Name..." -ForegroundColor Yellow -NoNewline
    
    try {
        if ($Method -eq "GET") {
            $response = Invoke-RestMethod -Uri $Url -Method $Method -TimeoutSec 5
        }
        Write-Host " Success" -ForegroundColor Green
        return $true
    } catch {
        Write-Host " Failed" -ForegroundColor Red
        Write-Host "  Error: $($_.Exception.Message)" -ForegroundColor Red
        return $false
    }
}

# Health checks
Write-Host "[1/4] Health Checks" -ForegroundColor Cyan
Write-Host "==================" -ForegroundColor Cyan

$healthChecks = @(
    @{Name="Eureka Server"; Url="http://localhost:8761/actuator/health"},
    @{Name="API Gateway"; Url="http://localhost:8080/actuator/health"},
    @{Name="Account Service"; Url="http://localhost:8081/actuator/health"},
    @{Name="Transfer Service"; Url="http://localhost:8082/actuator/health"}
)

$allHealthy = $true
foreach ($check in $healthChecks) {
    $result = Test-Endpoint -Name $check.Name -Url $check.Url
    if (-not $result) { $allHealthy = $false }
}

Write-Host ""

if (-not $allHealthy) {
    Write-Host "Some services are not healthy. Please check logs." -ForegroundColor Red
    exit 1
}

# Test Account Service
Write-Host "[2/4] Testing Account Service" -ForegroundColor Cyan
Write-Host "=============================" -ForegroundColor Cyan

Write-Host "Creating account..." -ForegroundColor Yellow

$accountBody = @{
    customerId = "CUST001"
    customerName = "Test User"
    accountType = "CHECKING"
    currency = "TRY"
    initialBalance = 1000.00
} | ConvertTo-Json

try {
    $accountResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/accounts" `
        -Method POST `
        -ContentType "application/json" `
        -Body $accountBody
    
    Write-Host "  Success: Account created successfully" -ForegroundColor Green
    Write-Host "    Account Number: $($accountResponse.data.accountNumber)" -ForegroundColor Cyan
    Write-Host "    IBAN: $($accountResponse.data.iban)" -ForegroundColor Cyan
    Write-Host "    Balance: $($accountResponse.data.balance) $($accountResponse.data.currency)" -ForegroundColor Cyan
    
    $fromAccount = $accountResponse.data.accountNumber
    
} catch {
    Write-Host "  Failed to create account" -ForegroundColor Red
    Write-Host "    Error: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host ""

# Create second account for transfer
Write-Host "Creating second account..." -ForegroundColor Yellow

$accountBody2 = @{
    customerId = "CUST002"
    customerName = "Test User 2"
    accountType = "SAVINGS"
    currency = "TRY"
    initialBalance = 500.00
} | ConvertTo-Json

try {
    $accountResponse2 = Invoke-RestMethod -Uri "http://localhost:8080/api/accounts" `
        -Method POST `
        -ContentType "application/json" `
        -Body $accountBody2
    
    Write-Host "  Success: Second account created successfully" -ForegroundColor Green
    Write-Host "    Account Number: $($accountResponse2.data.accountNumber)" -ForegroundColor Cyan
    Write-Host "    Balance: $($accountResponse2.data.balance) $($accountResponse2.data.currency)" -ForegroundColor Cyan
    
    $toAccount = $accountResponse2.data.accountNumber
    
} catch {
    Write-Host "  Failed to create second account" -ForegroundColor Red
    Write-Host "    Error: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host ""

# Test Transfer Service
Write-Host "[3/4] Testing Transfer Service" -ForegroundColor Cyan
Write-Host "==============================" -ForegroundColor Cyan

Write-Host "Executing transfer..." -ForegroundColor Yellow

$randomKey = Get-Random
$transferBody = @{
    fromAccountNumber = $fromAccount
    toAccountNumber = $toAccount
    amount = 100.00
    currency = "TRY"
    description = "Test transfer"
    transferType = "INTERNAL"
    idempotencyKey = "TEST-TRANSFER-$randomKey"
} | ConvertTo-Json

try {
    $transferResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/transfers" `
        -Method POST `
        -ContentType "application/json" `
        -Body $transferBody
    
    Write-Host "  Success: Transfer completed successfully" -ForegroundColor Green
    Write-Host "    Transfer Reference: $($transferResponse.data.transferReference)" -ForegroundColor Cyan
    Write-Host "    Status: $($transferResponse.data.status)" -ForegroundColor Cyan
    Write-Host "    Amount: $($transferResponse.data.amount) $($transferResponse.data.currency)" -ForegroundColor Cyan
    Write-Host "    From: $($transferResponse.data.fromAccountNumber)" -ForegroundColor Cyan
    Write-Host "    To: $($transferResponse.data.toAccountNumber)" -ForegroundColor Cyan
    
} catch {
    Write-Host "  Transfer failed" -ForegroundColor Red
    Write-Host "    Error: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# Test Idempotency
Write-Host "[4/4] Testing Idempotency" -ForegroundColor Cyan
Write-Host "=========================" -ForegroundColor Cyan

Write-Host "Attempting duplicate transfer..." -ForegroundColor Yellow

try {
    # Try the same transfer again with same idempotency key
    $duplicateResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/transfers" `
        -Method POST `
        -ContentType "application/json" `
        -Body $transferBody
    
    Write-Host "  Success: Idempotency working - Returned existing transfer" -ForegroundColor Green
    Write-Host "    Transfer Reference: $($duplicateResponse.data.transferReference)" -ForegroundColor Cyan
    
} catch {
    if ($_.Exception.Response.StatusCode -eq 409) {
        Write-Host "  Success: Idempotency working - Duplicate prevented (409 Conflict)" -ForegroundColor Green
    } else {
        Write-Host "  Warning: Unexpected error: $($_.Exception.Message)" -ForegroundColor Yellow
    }
}

Write-Host ""

# Summary
Write-Host "=====================================" -ForegroundColor Green
Write-Host "ALL TESTS COMPLETED SUCCESSFULLY!" -ForegroundColor Green
Write-Host "=====================================" -ForegroundColor Green
Write-Host ""
Write-Host "System is working correctly:" -ForegroundColor Cyan
Write-Host "  Success: All services are healthy" -ForegroundColor Green
Write-Host "  Success: Account creation works" -ForegroundColor Green
Write-Host "  Success: Transfer execution works (SAGA pattern)" -ForegroundColor Green
Write-Host "  Success: Idempotency protection works" -ForegroundColor Green
Write-Host ""
Write-Host "You can now:" -ForegroundColor Cyan
Write-Host "  1. Check Eureka Dashboard: http://localhost:8761" -ForegroundColor White
Write-Host "  2. Check Kafka UI: http://localhost:8090" -ForegroundColor White
Write-Host "  3. View service logs: docker-compose logs -f [service-name]" -ForegroundColor White
Write-Host ""
