# SWIFT Service API Test Script
# Tests all endpoints with authentication

Write-Host "====================================" -ForegroundColor Cyan
Write-Host "SWIFT Service API Tests" -ForegroundColor Cyan
Write-Host "====================================" -ForegroundColor Cyan
Write-Host ""

# Configuration
$BASE_URL = "http://localhost:8094"
$AUTH_URL = "http://localhost:8084"
$ACCOUNT_SERVICE_URL = "http://localhost:8081"

# Test counters
$testsPassed = 0
$testsFailed = 0

# Helper function to display test results
function Test-Result {
    param(
        [string]$TestName,
        [bool]$Passed,
        [string]$Message = ""
    )

    if ($Passed) {
        Write-Host "[PASS] $TestName" -ForegroundColor Green
        if ($Message) { Write-Host "       $Message" -ForegroundColor Gray }
        $script:testsPassed++
    }
    else {
        Write-Host "[FAIL] $TestName" -ForegroundColor Red
        if ($Message) { Write-Host "       $Message" -ForegroundColor Yellow }
        $script:testsFailed++
    }
}

# Helper function to make API calls
function Invoke-ApiRequest {
    param(
        [string]$Method,
        [string]$Uri,
        [object]$Body = $null,
        [string]$Token = $null,
        [int]$ExpectedStatus = 200
    )

    try {
        $headers = @{
            "Content-Type" = "application/json"
        }

        if ($Token) {
            $headers["Authorization"] = "Bearer $Token"
        }

        $params = @{
            Method = $Method
            Uri = $Uri
            Headers = $headers
            ErrorAction = "Stop"
        }

        if ($Body) {
            $params["Body"] = ($Body | ConvertTo-Json -Depth 10)
        }

        $response = Invoke-RestMethod @params
        return @{
            Success = $true
            Data = $response
            StatusCode = 200
        }
    }
    catch {
        $statusCode = 0
        if ($_.Exception.Response) {
            $statusCode = [int]$_.Exception.Response.StatusCode
        }

        return @{
            Success = $false
            Error = $_.Exception.Message
            StatusCode = $statusCode
        }
    }
}

# Test 1: Health Check
Write-Host "`n[TEST 1] Health Check" -ForegroundColor Yellow
$result = Invoke-ApiRequest -Method GET -Uri "$BASE_URL/actuator/health"
Test-Result -TestName "Service Health Check" `
    -Passed ($result.Success -and $result.Data.status -eq "UP") `
    -Message "Status: $($result.Data.status)"

# Test 2: Authentication - Register User
Write-Host "`n[TEST 2] User Registration" -ForegroundColor Yellow
$registerRequest = @{
    email = "swift.test@banking.com"
    password = "SwiftTest123!"
    firstName = "SWIFT"
    lastName = "Tester"
}
$result = Invoke-ApiRequest -Method POST -Uri "$AUTH_URL/auth/register" -Body $registerRequest
Test-Result -TestName "Register Test User" `
    -Passed $result.Success `
    -Message "User ID: $($result.Data.userId)"

# Test 3: Authentication - Login
Write-Host "`n[TEST 3] User Login" -ForegroundColor Yellow
$loginRequest = @{
    email = "swift.test@banking.com"
    password = "SwiftTest123!"
}
$result = Invoke-ApiRequest -Method POST -Uri "$AUTH_URL/auth/login" -Body $loginRequest
$accessToken = $result.Data.accessToken
Test-Result -TestName "User Login" `
    -Passed ($result.Success -and $accessToken) `
    -Message "Token received (length: $($accessToken.Length))"

if (-not $accessToken) {
    Write-Host "`n[ERROR] Cannot proceed without access token. Exiting." -ForegroundColor Red
    exit 1
}

# Test 4: Unauthorized Access (No Token)
Write-Host "`n[TEST 4] Unauthorized Access" -ForegroundColor Yellow
$result = Invoke-ApiRequest -Method GET -Uri "$BASE_URL/swift/transfers/statistics"
Test-Result -TestName "Access Without Token (Should Fail)" `
    -Passed ($result.StatusCode -eq 401) `
    -Message "Status Code: $($result.StatusCode)"

# Test 5: Get Statistics (Authorized)
Write-Host "`n[TEST 5] Get SWIFT Statistics" -ForegroundColor Yellow
$result = Invoke-ApiRequest -Method GET -Uri "$BASE_URL/swift/transfers/statistics" -Token $accessToken
Test-Result -TestName "Get Statistics" `
    -Passed $result.Success `
    -Message "Pending: $($result.Data.pendingCount), Completed: $($result.Data.completedCount)"

# Test 6: Create SWIFT Transfer
Write-Host "`n[TEST 6] Create SWIFT Transfer" -ForegroundColor Yellow
$createTransferRequest = @{
    internalAccountId = "ACC123456789"
    valueDate = "2026-01-20"
    currency = "USD"
    amount = 15000.00
    orderingCustomerName = "John Doe"
    orderingCustomerAddress = "123 Main St, New York"
    orderingCustomerAccount = "US1234567890"
    senderBic = "CHASUS33XXX"
    senderName = "JP Morgan Chase Bank"
    beneficiaryBankBic = "DEUTDEFFXXX"
    beneficiaryBankName = "Deutsche Bank AG"
    beneficiaryBankAddress = "Frankfurt, Germany"
    beneficiaryName = "Max Mustermann"
    beneficiaryAddress = "456 Berlin Str, Germany"
    beneficiaryAccount = "DE89370400440532013000"
    remittanceInfo = "Invoice Payment INV-2026-001"
    chargeType = "SHA"
}
$result = Invoke-ApiRequest -Method POST -Uri "$BASE_URL/swift/transfers" -Body $createTransferRequest -Token $accessToken
$transferReference = $result.Data.transactionReference
Test-Result -TestName "Create SWIFT Transfer" `
    -Passed ($result.Success -and $transferReference) `
    -Message "Reference: $transferReference, Amount: $($result.Data.amount) $($result.Data.currency)"

if (-not $transferReference) {
    Write-Host "`n[ERROR] Transfer creation failed. Skipping dependent tests." -ForegroundColor Red
    $transferReference = "SWFT000000000000"
}

# Test 7: Get Transfer by Reference
Write-Host "`n[TEST 7] Get Transfer by Reference" -ForegroundColor Yellow
$result = Invoke-ApiRequest -Method GET -Uri "$BASE_URL/swift/transfers/$transferReference" -Token $accessToken
Test-Result -TestName "Get Transfer by Reference" `
    -Passed ($result.Success -and $result.Data.transactionReference -eq $transferReference) `
    -Message "Status: $($result.Data.status), Amount: $($result.Data.amount)"

# Test 8: Get Transfers by Account
Write-Host "`n[TEST 8] Get Transfers by Account" -ForegroundColor Yellow
$result = Invoke-ApiRequest -Method GET -Uri "$BASE_URL/swift/transfers/account/ACC123456789" -Token $accessToken
Test-Result -TestName "Get Transfers by Account" `
    -Passed $result.Success `
    -Message "Found $($result.Data.Count) transfer(s)"

# Test 9: Get Transfers by Status (PENDING)
Write-Host "`n[TEST 9] Get Transfers by Status" -ForegroundColor Yellow
$result = Invoke-ApiRequest -Method GET -Uri "$BASE_URL/swift/transfers/status/PENDING?page=0&size=10" -Token $accessToken
Test-Result -TestName "Get Transfers by Status (PENDING)" `
    -Passed $result.Success `
    -Message "Found $($result.Data.totalElements) pending transfer(s)"

# Test 10: Process Transfer
Write-Host "`n[TEST 10] Process SWIFT Transfer" -ForegroundColor Yellow
$result = Invoke-ApiRequest -Method POST -Uri "$BASE_URL/swift/transfers/$transferReference/process" -Token $accessToken
Test-Result -TestName "Process Transfer" `
    -Passed ($result.Success -and $result.Data.status -ne "PENDING") `
    -Message "Status: $($result.Data.status), MT103 Generated: $($result.Data.mt103Message -ne $null)"

# Test 11: Create Transfer with Invalid BIC
Write-Host "`n[TEST 11] Validate BIC Code (Should Fail)" -ForegroundColor Yellow
$invalidBicRequest = @{
    internalAccountId = "ACC123456789"
    valueDate = "2026-01-20"
    currency = "EUR"
    amount = 5000.00
    orderingCustomerName = "Jane Smith"
    senderBic = "INVALID"
    beneficiaryBankBic = "DEUTDEFFXXX"
    beneficiaryName = "Test Beneficiary"
    beneficiaryAccount = "DE89370400440532013000"
    chargeType = "OUR"
}
$result = Invoke-ApiRequest -Method POST -Uri "$BASE_URL/swift/transfers" -Body $invalidBicRequest -Token $accessToken
Test-Result -TestName "Invalid BIC Validation" `
    -Passed (-not $result.Success) `
    -Message "Correctly rejected invalid BIC"

# Test 12: Create Transfer with Invalid Amount
Write-Host "`n[TEST 12] Validate Amount (Should Fail)" -ForegroundColor Yellow
$invalidAmountRequest = $createTransferRequest.Clone()
$invalidAmountRequest.amount = -1000.00
$result = Invoke-ApiRequest -Method POST -Uri "$BASE_URL/swift/transfers" -Body $invalidAmountRequest -Token $accessToken
Test-Result -TestName "Invalid Amount Validation" `
    -Passed (-not $result.Success) `
    -Message "Correctly rejected negative amount"

# Test 13: Create Transfer with Missing Required Fields
Write-Host "`n[TEST 13] Validate Required Fields (Should Fail)" -ForegroundColor Yellow
$incompleteRequest = @{
    internalAccountId = "ACC123456789"
    amount = 1000.00
    # Missing currency, BICs, beneficiary details
}
$result = Invoke-ApiRequest -Method POST -Uri "$BASE_URL/swift/transfers" -Body $incompleteRequest -Token $accessToken
Test-Result -TestName "Missing Required Fields Validation" `
    -Passed (-not $result.Success) `
    -Message "Correctly rejected incomplete request"

# Test 14: Create Multiple Transfers (Different Currencies)
Write-Host "`n[TEST 14] Create Multiple Transfers" -ForegroundColor Yellow
$currencies = @("USD", "EUR", "GBP")
$successCount = 0

foreach ($currency in $currencies) {
    $request = $createTransferRequest.Clone()
    $request.currency = $currency
    $request.amount = 10000.00
    $result = Invoke-ApiRequest -Method POST -Uri "$BASE_URL/swift/transfers" -Body $request -Token $accessToken
    if ($result.Success) { $successCount++ }
}

Test-Result -TestName "Create Multiple Currency Transfers" `
    -Passed ($successCount -eq 3) `
    -Message "Successfully created $successCount/$($currencies.Count) transfers"

# Test 15: Get Updated Statistics
Write-Host "`n[TEST 15] Verify Statistics Update" -ForegroundColor Yellow
$result = Invoke-ApiRequest -Method GET -Uri "$BASE_URL/swift/transfers/statistics" -Token $accessToken
$totalTransfers = $result.Data.pendingCount + $result.Data.processingCount + $result.Data.completedCount + $result.Data.failedCount
Test-Result -TestName "Statistics Updated" `
    -Passed ($result.Success -and $totalTransfers -gt 0) `
    -Message "Total Transfers: $totalTransfers, Pending: $($result.Data.pendingCount)"

# Test 16: Complete Transfer
Write-Host "`n[TEST 16] Complete SWIFT Transfer" -ForegroundColor Yellow
$result = Invoke-ApiRequest -Method POST -Uri "$BASE_URL/swift/transfers/$transferReference/complete?settlementReference=SETTLE-2026-001" -Token $accessToken
Test-Result -TestName "Complete Transfer" `
    -Passed ($result.Success) `
    -Message "Status: $($result.Data.status), Settlement Ref: $($result.Data.settlementReference)"

# Test 17: Fail Transfer (Create new one to fail)
Write-Host "`n[TEST 17] Fail SWIFT Transfer" -ForegroundColor Yellow
$createResult = Invoke-ApiRequest -Method POST -Uri "$BASE_URL/swift/transfers" -Body $createTransferRequest -Token $accessToken
if ($createResult.Success) {
    $newReference = $createResult.Data.transactionReference
    $result = Invoke-ApiRequest -Method POST -Uri "$BASE_URL/swift/transfers/$newReference/fail?reason=Compliance+check+failed" -Token $accessToken
    Test-Result -TestName "Fail Transfer" `
        -Passed ($result.Success -and $result.Data.status -eq "FAILED") `
        -Message "Status: $($result.Data.status), Reason: $($result.Data.failureReason)"
}
else {
    Test-Result -TestName "Fail Transfer" -Passed $false -Message "Could not create transfer to fail"
}

# Test 18: Access with Expired Token
Write-Host "`n[TEST 18] Expired Token Validation" -ForegroundColor Yellow
$expiredToken = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIiwiZXhwIjoxNjQwOTk1MjAwfQ.invalid"
$result = Invoke-ApiRequest -Method GET -Uri "$BASE_URL/swift/transfers/statistics" -Token $expiredToken
Test-Result -TestName "Reject Expired Token" `
    -Passed ($result.StatusCode -eq 401) `
    -Message "Correctly rejected expired token"

# Test 19: Access Swagger UI (Public)
Write-Host "`n[TEST 19] Public Swagger UI Access" -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "$BASE_URL/swagger-ui.html" -ErrorAction Stop
    Test-Result -TestName "Swagger UI Access" `
        -Passed ($response.StatusCode -eq 200 -or $response.StatusCode -eq 302) `
        -Message "Status Code: $($response.StatusCode)"
}
catch {
    $statusCode = [int]$_.Exception.Response.StatusCode
    Test-Result -TestName "Swagger UI Access" `
        -Passed ($statusCode -eq 200 -or $statusCode -eq 302) `
        -Message "Status Code: $statusCode"
}

# Test 20: Access OpenAPI Docs (Public)
Write-Host "`n[TEST 20] Public OpenAPI Docs Access" -ForegroundColor Yellow
$result = Invoke-ApiRequest -Method GET -Uri "$BASE_URL/v3/api-docs"
Test-Result -TestName "OpenAPI Docs Access" `
    -Passed $result.Success `
    -Message "API documentation available"

# Summary
Write-Host "`n====================================" -ForegroundColor Cyan
Write-Host "Test Summary" -ForegroundColor Cyan
Write-Host "====================================" -ForegroundColor Cyan
Write-Host "Tests Passed: $testsPassed" -ForegroundColor Green
Write-Host "Tests Failed: $testsFailed" -ForegroundColor $(if ($testsFailed -eq 0) { "Green" } else { "Red" })
Write-Host "Total Tests:  $($testsPassed + $testsFailed)"
Write-Host ""

if ($testsFailed -eq 0) {
    Write-Host "All tests passed! ✓" -ForegroundColor Green
    exit 0
}
else {
    Write-Host "Some tests failed! ✗" -ForegroundColor Red
    exit 1
}
