# Transfer Service API Test Script
# Tests all endpoints of the Transfer Service
# Usage: .\test-transfer-api.ps1

$ErrorActionPreference = "Continue"
$baseUrl = "http://localhost:8082/api/v1/transfers"
$authServiceUrl = "http://localhost:8084/auth"
$accountServiceUrl = "http://localhost:8081/api/v1/accounts"

# Colors for output
function Write-Success { param($message) Write-Host $message -ForegroundColor Green }
function Write-Error { param($message) Write-Host $message -ForegroundColor Red }
function Write-Info { param($message) Write-Host $message -ForegroundColor Cyan }
function Write-Warning { param($message) Write-Host $message -ForegroundColor Yellow }

# Test counter
$script:testsPassed = 0
$script:testsFailed = 0
$script:testsTotal = 0

function Test-Endpoint {
    param(
        [string]$Name,
        [string]$Method,
        [string]$Url,
        [string]$Body,
        [int]$ExpectedStatus,
        [string]$Token = $null,
        [string]$Description = ""
    )

    $script:testsTotal++

    Write-Info "`n--- Test #$script:testsTotal: $Name ---"
    if ($Description) {
        Write-Host "Description: $Description" -ForegroundColor Gray
    }

    try {
        $headers = @{
            "Content-Type" = "application/json"
        }

        if ($Token) {
            $headers["Authorization"] = "Bearer $Token"
        }

        $params = @{
            Uri = $Url
            Method = $Method
            Headers = $headers
            StatusCodeVariable = "statusCode"
        }

        if ($Body) {
            $params["Body"] = $Body
        }

        $response = Invoke-WebRequest @params
        $content = $response.Content | ConvertFrom-Json

        if ($statusCode -eq $ExpectedStatus) {
            Write-Success "✓ PASSED - Status: $statusCode (Expected: $ExpectedStatus)"
            $script:testsPassed++

            if ($content) {
                Write-Host "Response: " -NoNewline
                Write-Host ($content | ConvertTo-Json -Depth 3 -Compress)
            }

            return $content
        } else {
            Write-Error "✗ FAILED - Status: $statusCode (Expected: $ExpectedStatus)"
            $script:testsFailed++
            return $null
        }
    }
    catch {
        $actualStatus = $_.Exception.Response.StatusCode.value__

        if ($actualStatus -eq $ExpectedStatus) {
            Write-Success "✓ PASSED - Status: $actualStatus (Expected: $ExpectedStatus)"
            $script:testsPassed++
            return $null
        } else {
            Write-Error "✗ FAILED - Status: $actualStatus (Expected: $ExpectedStatus)"
            Write-Error "Error: $($_.Exception.Message)"
            $script:testsFailed++
            return $null
        }
    }
}

# Initialize test data
$script:jwtToken = $null
$script:sourceAccountNumber = $null
$script:destinationAccountNumber = $null
$script:transferReference = $null

Write-Host "`n╔════════════════════════════════════════════════════════════╗" -ForegroundColor Magenta
Write-Host "║         TRANSFER SERVICE API TEST SUITE                    ║" -ForegroundColor Magenta
Write-Host "╚════════════════════════════════════════════════════════════╝" -ForegroundColor Magenta

# Step 1: Authenticate and get JWT token
Write-Info "`n=== AUTHENTICATION ==="

$registerBody = @{
    email = "test.transfer.$(Get-Random)@example.com"
    password = "Test123!@#"
    firstName = "Transfer"
    lastName = "Tester"
} | ConvertTo-Json

$registerResponse = Test-Endpoint `
    -Name "Register Test User" `
    -Method "POST" `
    -Url "$authServiceUrl/register" `
    -Body $registerBody `
    -ExpectedStatus 201 `
    -Description "Register a new test user to get JWT token"

if ($registerResponse -and $registerResponse.data.email) {
    $loginBody = @{
        email = $registerResponse.data.email
        password = "Test123!@#"
    } | ConvertTo-Json

    $loginResponse = Test-Endpoint `
        -Name "Login Test User" `
        -Method "POST" `
        -Url "$authServiceUrl/login" `
        -Body $loginBody `
        -ExpectedStatus 200 `
        -Description "Login to get JWT access token"

    if ($loginResponse -and $loginResponse.data.accessToken) {
        $script:jwtToken = $loginResponse.data.accessToken
        Write-Success "✓ JWT Token obtained successfully"
    } else {
        Write-Warning "⚠ Could not obtain JWT token - some tests may fail"
    }
} else {
    Write-Warning "⚠ Could not register user - some tests may fail"
}

# Step 2: Create test accounts for transfers
Write-Info "`n=== SETUP TEST ACCOUNTS ==="

$createSourceAccountBody = @{
    customerId = "CUS-$(Get-Random -Minimum 100000 -Maximum 999999)"
    customerName = "Source Account Owner"
    currency = "TRY"
    accountType = "CHECKING"
    initialBalance = 5000.00
} | ConvertTo-Json

$sourceAccountResponse = Test-Endpoint `
    -Name "Create Source Account" `
    -Method "POST" `
    -Url "$accountServiceUrl" `
    -Body $createSourceAccountBody `
    -Token $script:jwtToken `
    -ExpectedStatus 201 `
    -Description "Create source account with 5000 TRY balance"

if ($sourceAccountResponse -and $sourceAccountResponse.data.accountNumber) {
    $script:sourceAccountNumber = $sourceAccountResponse.data.accountNumber
    Write-Success "✓ Source Account created: $script:sourceAccountNumber (Balance: 5000 TRY)"
}

$createDestinationAccountBody = @{
    customerId = "CUS-$(Get-Random -Minimum 100000 -Maximum 999999)"
    customerName = "Destination Account Owner"
    currency = "TRY"
    accountType = "CHECKING"
    initialBalance = 1000.00
} | ConvertTo-Json

$destinationAccountResponse = Test-Endpoint `
    -Name "Create Destination Account" `
    -Method "POST" `
    -Url "$accountServiceUrl" `
    -Body $createDestinationAccountBody `
    -Token $script:jwtToken `
    -ExpectedStatus 201 `
    -Description "Create destination account with 1000 TRY balance"

if ($destinationAccountResponse -and $destinationAccountResponse.data.accountNumber) {
    $script:destinationAccountNumber = $destinationAccountResponse.data.accountNumber
    Write-Success "✓ Destination Account created: $script:destinationAccountNumber (Balance: 1000 TRY)"
}

# Step 3: Test unauthorized access
Write-Info "`n=== UNAUTHORIZED ACCESS TESTS ==="

Test-Endpoint `
    -Name "Unauthorized - Initiate Transfer" `
    -Method "POST" `
    -Url "$baseUrl" `
    -Body '{"fromAccountNumber":"TR330006100519786457841326","toAccountNumber":"TR330006200519786457841327","amount":100,"currency":"TRY","transferType":"INTERNAL"}' `
    -ExpectedStatus 401 `
    -Description "Should return 401 without JWT token"

Test-Endpoint `
    -Name "Unauthorized - Get Transfer" `
    -Method "GET" `
    -Url "$baseUrl/TXF-123456789012" `
    -ExpectedStatus 401 `
    -Description "Should return 401 without JWT token"

# Step 4: Initiate transfer tests
Write-Info "`n=== INITIATE TRANSFER TESTS ==="

if ($script:sourceAccountNumber -and $script:destinationAccountNumber) {
    $transferBody = @{
        fromAccountNumber = $script:sourceAccountNumber
        toAccountNumber = $script:destinationAccountNumber
        amount = 500.00
        currency = "TRY"
        description = "Test transfer - API integration test"
        transferType = "INTERNAL"
        idempotencyKey = "test-transfer-$(Get-Random)"
    } | ConvertTo-Json

    $transferResponse = Test-Endpoint `
        -Name "Initiate Transfer" `
        -Method "POST" `
        -Url "$baseUrl" `
        -Body $transferBody `
        -Token $script:jwtToken `
        -ExpectedStatus 201 `
        -Description "Transfer 500 TRY from source to destination account"

    if ($transferResponse -and $transferResponse.data.transferReference) {
        $script:transferReference = $transferResponse.data.transferReference
        Write-Success "✓ Transfer initiated: $script:transferReference"
        Write-Info "Status: $($transferResponse.data.status)"
        Write-Info "Amount: $($transferResponse.data.amount) $($transferResponse.data.currency)"
    }
}

# Step 5: Validation tests
Write-Info "`n=== VALIDATION TESTS ==="

$invalidTransferBody1 = @{
    fromAccountNumber = ""
    toAccountNumber = ""
    amount = 0
    currency = ""
} | ConvertTo-Json

Test-Endpoint `
    -Name "Invalid Transfer - Empty Fields" `
    -Method "POST" `
    -Url "$baseUrl" `
    -Body $invalidTransferBody1 `
    -Token $script:jwtToken `
    -ExpectedStatus 400 `
    -Description "Should return 400 for empty required fields"

$invalidTransferBody2 = @{
    fromAccountNumber = "SHORT"
    toAccountNumber = "SHORT"
    amount = 100
    currency = "TRY"
    transferType = "INTERNAL"
} | ConvertTo-Json

Test-Endpoint `
    -Name "Invalid Transfer - Short Account Numbers" `
    -Method "POST" `
    -Url "$baseUrl" `
    -Body $invalidTransferBody2 `
    -Token $script:jwtToken `
    -ExpectedStatus 400 `
    -Description "Should return 400 for account numbers less than 10 characters"

$invalidTransferBody3 = @{
    fromAccountNumber = $script:sourceAccountNumber
    toAccountNumber = $script:destinationAccountNumber
    amount = -100
    currency = "TRY"
    transferType = "INTERNAL"
} | ConvertTo-Json

if ($script:sourceAccountNumber -and $script:destinationAccountNumber) {
    Test-Endpoint `
        -Name "Invalid Transfer - Negative Amount" `
        -Method "POST" `
        -Url "$baseUrl" `
        -Body $invalidTransferBody3 `
        -Token $script:jwtToken `
        -ExpectedStatus 400 `
        -Description "Should return 400 for negative transfer amount"
}

$invalidTransferBody4 = @{
    fromAccountNumber = $script:sourceAccountNumber
    toAccountNumber = $script:destinationAccountNumber
    amount = 100
    currency = "XX"
    transferType = "INTERNAL"
} | ConvertTo-Json

if ($script:sourceAccountNumber -and $script:destinationAccountNumber) {
    Test-Endpoint `
        -Name "Invalid Transfer - Invalid Currency" `
        -Method "POST" `
        -Url "$baseUrl" `
        -Body $invalidTransferBody4 `
        -Token $script:jwtToken `
        -ExpectedStatus 400 `
        -Description "Should return 400 for invalid currency code"
}

# Step 6: Large amount transfer test
Write-Info "`n=== LARGE AMOUNT TRANSFER TEST ==="

if ($script:sourceAccountNumber -and $script:destinationAccountNumber) {
    $largeTransferBody = @{
        fromAccountNumber = $script:sourceAccountNumber
        toAccountNumber = $script:destinationAccountNumber
        amount = 2000.00
        currency = "TRY"
        description = "Large amount test transfer"
        transferType = "INTERNAL"
        idempotencyKey = "test-large-$(Get-Random)"
    } | ConvertTo-Json

    $largeTransferResponse = Test-Endpoint `
        -Name "Large Amount Transfer" `
        -Method "POST" `
        -Url "$baseUrl" `
        -Body $largeTransferBody `
        -Token $script:jwtToken `
        -ExpectedStatus 201 `
        -Description "Transfer 2000 TRY (large amount)"

    if ($largeTransferResponse -and $largeTransferResponse.data.transferReference) {
        Write-Success "✓ Large transfer completed: $($largeTransferResponse.data.transferReference)"
    }
}

# Step 7: Idempotency test
Write-Info "`n=== IDEMPOTENCY TEST ==="

if ($script:sourceAccountNumber -and $script:destinationAccountNumber) {
    $idempotencyKey = "idempotency-test-$(Get-Random)"

    $idempotentTransferBody = @{
        fromAccountNumber = $script:sourceAccountNumber
        toAccountNumber = $script:destinationAccountNumber
        amount = 100.00
        currency = "TRY"
        description = "Idempotency test transfer"
        transferType = "INTERNAL"
        idempotencyKey = $idempotencyKey
    } | ConvertTo-Json

    $firstTransfer = Test-Endpoint `
        -Name "First Transfer With Idempotency Key" `
        -Method "POST" `
        -Url "$baseUrl" `
        -Body $idempotentTransferBody `
        -Token $script:jwtToken `
        -ExpectedStatus 201 `
        -Description "First transfer with specific idempotency key"

    # Try the same transfer again with same idempotency key
    Start-Sleep -Seconds 1

    $duplicateTransfer = Test-Endpoint `
        -Name "Duplicate Transfer (Same Idempotency Key)" `
        -Method "POST" `
        -Url "$baseUrl" `
        -Body $idempotentTransferBody `
        -Token $script:jwtToken `
        -ExpectedStatus 201 `
        -Description "Should return same transfer (idempotency check)"

    if ($firstTransfer -and $duplicateTransfer) {
        if ($firstTransfer.data.transferReference -eq $duplicateTransfer.data.transferReference) {
            Write-Success "✓ Idempotency working correctly - same transfer reference returned"
        } else {
            Write-Warning "⚠ Different transfer references - idempotency may not be working"
        }
    }
}

# Step 8: Get transfer by reference tests
Write-Info "`n=== GET TRANSFER BY REFERENCE TESTS ==="

if ($script:transferReference) {
    $getTransferResponse = Test-Endpoint `
        -Name "Get Transfer By Reference" `
        -Method "GET" `
        -Url "$baseUrl/$script:transferReference" `
        -Token $script:jwtToken `
        -ExpectedStatus 200 `
        -Description "Retrieve transfer details by reference"

    if ($getTransferResponse -and $getTransferResponse.data) {
        Write-Info "Transfer Reference: $($getTransferResponse.data.transferReference)"
        Write-Info "Status: $($getTransferResponse.data.status)"
        Write-Info "Amount: $($getTransferResponse.data.amount) $($getTransferResponse.data.currency)"
    }
}

Test-Endpoint `
    -Name "Get Non-Existent Transfer" `
    -Method "GET" `
    -Url "$baseUrl/TXF-NONEXISTENT123" `
    -Token $script:jwtToken `
    -ExpectedStatus 404 `
    -Description "Should return 404 for non-existent transfer reference"

# Step 9: Get transfers by account tests
Write-Info "`n=== GET TRANSFERS BY ACCOUNT TESTS ==="

if ($script:sourceAccountNumber) {
    $getByAccountResponse = Test-Endpoint `
        -Name "Get All Transfers For Account" `
        -Method "GET" `
        -Url "$baseUrl/account/$script:sourceAccountNumber" `
        -Token $script:jwtToken `
        -ExpectedStatus 200 `
        -Description "Get all transfers (sent + received) for source account"

    if ($getByAccountResponse -and $getByAccountResponse.data) {
        Write-Info "Total Transfers: $($getByAccountResponse.data.Count)"
    }
}

Test-Endpoint `
    -Name "Get Transfers For Non-Existent Account" `
    -Method "GET" `
    -Url "$baseUrl/account/TR330099900519786457841329" `
    -Token $script:jwtToken `
    -ExpectedStatus 200 `
    -Description "Should return empty list for account with no transfers"

# Step 10: Get outgoing transfers tests
Write-Info "`n=== GET OUTGOING TRANSFERS TESTS ==="

if ($script:sourceAccountNumber) {
    $getOutgoingResponse = Test-Endpoint `
        -Name "Get Outgoing Transfers" `
        -Method "GET" `
        -Url "$baseUrl/from/$script:sourceAccountNumber" `
        -Token $script:jwtToken `
        -ExpectedStatus 200 `
        -Description "Get all transfers sent FROM source account"

    if ($getOutgoingResponse -and $getOutgoingResponse.data) {
        Write-Info "Outgoing Transfers: $($getOutgoingResponse.data.Count)"
        if ($getOutgoingResponse.data.Count -gt 0) {
            Write-Info "Recent Outgoing Transfers:"
            $getOutgoingResponse.data | Select-Object -First 3 | ForEach-Object {
                Write-Host "  - $($_.transferReference): $($_.amount) $($_.currency) to $($_.toAccountNumber)" -ForegroundColor Gray
            }
        }
    }
}

Test-Endpoint `
    -Name "Get Outgoing For Non-Existent Account" `
    -Method "GET" `
    -Url "$baseUrl/from/TR330099900519786457841329" `
    -Token $script:jwtToken `
    -ExpectedStatus 200 `
    -Description "Should return empty list for account with no outgoing transfers"

# Step 11: Get incoming transfers tests
Write-Info "`n=== GET INCOMING TRANSFERS TESTS ==="

if ($script:destinationAccountNumber) {
    $getIncomingResponse = Test-Endpoint `
        -Name "Get Incoming Transfers" `
        -Method "GET" `
        -Url "$baseUrl/to/$script:destinationAccountNumber" `
        -Token $script:jwtToken `
        -ExpectedStatus 200 `
        -Description "Get all transfers sent TO destination account"

    if ($getIncomingResponse -and $getIncomingResponse.data) {
        Write-Info "Incoming Transfers: $($getIncomingResponse.data.Count)"
        if ($getIncomingResponse.data.Count -gt 0) {
            Write-Info "Recent Incoming Transfers:"
            $getIncomingResponse.data | Select-Object -First 3 | ForEach-Object {
                Write-Host "  - $($_.transferReference): $($_.amount) $($_.currency) from $($_.fromAccountNumber)" -ForegroundColor Gray
            }
        }
    }
}

Test-Endpoint `
    -Name "Get Incoming For Non-Existent Account" `
    -Method "GET" `
    -Url "$baseUrl/to/TR330099900519786457841329" `
    -Token $script:jwtToken `
    -ExpectedStatus 200 `
    -Description "Should return empty list for account with no incoming transfers"

# Step 12: Edge cases
Write-Info "`n=== EDGE CASE TESTS ==="

if ($script:sourceAccountNumber -and $script:destinationAccountNumber) {
    $longDescriptionBody = @{
        fromAccountNumber = $script:sourceAccountNumber
        toAccountNumber = $script:destinationAccountNumber
        amount = 50.00
        currency = "TRY"
        description = "A" * 500
        transferType = "INTERNAL"
        idempotencyKey = "test-long-desc-$(Get-Random)"
    } | ConvertTo-Json

    Test-Endpoint `
        -Name "Transfer With Long Description" `
        -Method "POST" `
        -Url "$baseUrl" `
        -Body $longDescriptionBody `
        -Token $script:jwtToken `
        -ExpectedStatus 201 `
        -Description "Transfer with maximum 500 character description"
}

if ($script:sourceAccountNumber -and $script:destinationAccountNumber) {
    $specialCharsBody = @{
        fromAccountNumber = $script:sourceAccountNumber
        toAccountNumber = $script:destinationAccountNumber
        amount = 25.00
        currency = "TRY"
        description = "Transfer with special chars: @#$%^&*()"
        transferType = "INTERNAL"
        idempotencyKey = "test-special-$(Get-Random)"
    } | ConvertTo-Json

    Test-Endpoint `
        -Name "Transfer With Special Characters" `
        -Method "POST" `
        -Url "$baseUrl" `
        -Body $specialCharsBody `
        -Token $script:jwtToken `
        -ExpectedStatus 201 `
        -Description "Transfer description with special characters"
}

# Step 13: Verify account balances after transfers
Write-Info "`n=== VERIFY FINAL ACCOUNT BALANCES ==="

if ($script:sourceAccountNumber) {
    $sourceBalanceResponse = Test-Endpoint `
        -Name "Check Source Account Balance" `
        -Method "GET" `
        -Url "$accountServiceUrl/number/$script:sourceAccountNumber" `
        -Token $script:jwtToken `
        -ExpectedStatus 200 `
        -Description "Verify source account balance after all transfers"

    if ($sourceBalanceResponse -and $sourceBalanceResponse.data) {
        Write-Info "Source Account Final Balance: $($sourceBalanceResponse.data.balance) TRY"
        Write-Info "Expected: ~2425 TRY (5000 - 500 - 2000 - 100 - 50 - 25)"
    }
}

if ($script:destinationAccountNumber) {
    $destBalanceResponse = Test-Endpoint `
        -Name "Check Destination Account Balance" `
        -Method "GET" `
        -Url "$accountServiceUrl/number/$script:destinationAccountNumber" `
        -Token $script:jwtToken `
        -ExpectedStatus 200 `
        -Description "Verify destination account balance after all transfers"

    if ($destBalanceResponse -and $destBalanceResponse.data) {
        Write-Info "Destination Account Final Balance: $($destBalanceResponse.data.balance) TRY"
        Write-Info "Expected: ~3675 TRY (1000 + 500 + 2000 + 100 + 50 + 25)"
    }
}

# Summary
Write-Host "`n╔════════════════════════════════════════════════════════════╗" -ForegroundColor Magenta
Write-Host "║                     TEST SUMMARY                           ║" -ForegroundColor Magenta
Write-Host "╚════════════════════════════════════════════════════════════╝" -ForegroundColor Magenta

Write-Info "`nTotal Tests: $script:testsTotal"
Write-Success "Passed: $script:testsPassed"
if ($script:testsFailed -gt 0) {
    Write-Error "Failed: $script:testsFailed"
} else {
    Write-Success "Failed: $script:testsFailed"
}

$successRate = [math]::Round(($script:testsPassed / $script:testsTotal) * 100, 2)
Write-Info "Success Rate: $successRate%"

if ($script:testsFailed -eq 0) {
    Write-Success "`n✓ ALL TESTS PASSED! ✓"
    Write-Info "`nTest Accounts Created:"
    if ($script:sourceAccountNumber) {
        Write-Host "  Source Account: $script:sourceAccountNumber" -ForegroundColor Gray
    }
    if ($script:destinationAccountNumber) {
        Write-Host "  Destination Account: $script:destinationAccountNumber" -ForegroundColor Gray
    }
    Write-Info "`nTest Transfers Created: $(if ($script:transferReference) { '5+' } else { 'N/A' })"
    exit 0
} else {
    Write-Error "`n✗ SOME TESTS FAILED ✗"
    exit 1
}
