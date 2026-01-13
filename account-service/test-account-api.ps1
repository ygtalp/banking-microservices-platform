# Account Service API Test Script
# Tests all endpoints of the Account Service
# Usage: .\test-account-api.ps1

$ErrorActionPreference = "Continue"
$baseUrl = "http://localhost:8081/api/v1/accounts"
$authServiceUrl = "http://localhost:8084/auth"

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
$script:testAccountNumber = $null
$script:testAccountNumber2 = $null
$script:jwtToken = $null

Write-Host "`n╔════════════════════════════════════════════════════════════╗" -ForegroundColor Magenta
Write-Host "║         ACCOUNT SERVICE API TEST SUITE                     ║" -ForegroundColor Magenta
Write-Host "╚════════════════════════════════════════════════════════════╝" -ForegroundColor Magenta

# Step 1: Authenticate and get JWT token
Write-Info "`n=== AUTHENTICATION ==="

$registerBody = @{
    email = "test.account.$(Get-Random)@example.com"
    password = "Test123!@#"
    firstName = "Test"
    lastName = "User"
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

# Step 2: Test unauthorized access
Write-Info "`n=== UNAUTHORIZED ACCESS TESTS ==="

Test-Endpoint `
    -Name "Unauthorized - Get Account" `
    -Method "GET" `
    -Url "$baseUrl/1" `
    -ExpectedStatus 401 `
    -Description "Should return 401 without JWT token"

Test-Endpoint `
    -Name "Unauthorized - Create Account" `
    -Method "POST" `
    -Url "$baseUrl" `
    -Body '{"customerId":"CUS-123","customerName":"Test","currency":"TRY","accountType":"CHECKING"}' `
    -ExpectedStatus 401 `
    -Description "Should return 401 without JWT token"

# Step 3: Create accounts (requires ADMIN/MANAGER role)
Write-Info "`n=== ACCOUNT CREATION TESTS ==="

$createAccountBody1 = @{
    customerId = "CUS-$(Get-Random -Minimum 100000 -Maximum 999999)"
    customerName = "John Doe"
    currency = "TRY"
    accountType = "CHECKING"
    initialBalance = 1000.00
} | ConvertTo-Json

$createResponse1 = Test-Endpoint `
    -Name "Create Account 1" `
    -Method "POST" `
    -Url "$baseUrl" `
    -Body $createAccountBody1 `
    -Token $script:jwtToken `
    -ExpectedStatus 201 `
    -Description "Create first test account with TRY currency"

if ($createResponse1 -and $createResponse1.data.accountNumber) {
    $script:testAccountNumber = $createResponse1.data.accountNumber
    Write-Success "✓ Account 1 created: $script:testAccountNumber"
}

$createAccountBody2 = @{
    customerId = "CUS-$(Get-Random -Minimum 100000 -Maximum 999999)"
    customerName = "Jane Smith"
    currency = "USD"
    accountType = "SAVINGS"
    initialBalance = 2000.00
} | ConvertTo-Json

$createResponse2 = Test-Endpoint `
    -Name "Create Account 2" `
    -Method "POST" `
    -Url "$baseUrl" `
    -Body $createAccountBody2 `
    -Token $script:jwtToken `
    -ExpectedStatus 201 `
    -Description "Create second test account with USD currency"

if ($createResponse2 -and $createResponse2.data.accountNumber) {
    $script:testAccountNumber2 = $createResponse2.data.accountNumber
    Write-Success "✓ Account 2 created: $script:testAccountNumber2"
}

# Step 4: Validation tests
Write-Info "`n=== VALIDATION TESTS ==="

$invalidAccountBody = @{
    customerId = ""
    customerName = ""
    currency = "INVALID"
    accountType = "INVALID"
} | ConvertTo-Json

Test-Endpoint `
    -Name "Invalid Account Data" `
    -Method "POST" `
    -Url "$baseUrl" `
    -Body $invalidAccountBody `
    -Token $script:jwtToken `
    -ExpectedStatus 400 `
    -Description "Should return 400 for invalid account data"

# Step 5: Get account tests
Write-Info "`n=== GET ACCOUNT TESTS ==="

if ($createResponse1 -and $createResponse1.data.id) {
    Test-Endpoint `
        -Name "Get Account By ID" `
        -Method "GET" `
        -Url "$baseUrl/$($createResponse1.data.id)" `
        -Token $script:jwtToken `
        -ExpectedStatus 200 `
        -Description "Retrieve account by database ID"
}

if ($script:testAccountNumber) {
    Test-Endpoint `
        -Name "Get Account By Number" `
        -Method "GET" `
        -Url "$baseUrl/number/$script:testAccountNumber" `
        -Token $script:jwtToken `
        -ExpectedStatus 200 `
        -Description "Retrieve account by account number (IBAN)"
}

Test-Endpoint `
    -Name "Get Non-Existent Account" `
    -Method "GET" `
    -Url "$baseUrl/999999" `
    -Token $script:jwtToken `
    -ExpectedStatus 404 `
    -Description "Should return 404 for non-existent account"

# Step 6: Get accounts by customer ID
Write-Info "`n=== GET ACCOUNTS BY CUSTOMER TESTS ==="

if ($createResponse1 -and $createResponse1.data.customerId) {
    Test-Endpoint `
        -Name "Get Accounts By Customer ID" `
        -Method "GET" `
        -Url "$baseUrl/customer/$($createResponse1.data.customerId)" `
        -Token $script:jwtToken `
        -ExpectedStatus 200 `
        -Description "Retrieve all accounts for a specific customer"
}

Test-Endpoint `
    -Name "Get Accounts For Non-Existent Customer" `
    -Method "GET" `
    -Url "$baseUrl/customer/CUS-NONEXISTENT" `
    -Token $script:jwtToken `
    -ExpectedStatus 200 `
    -Description "Should return empty array for customer with no accounts"

# Step 7: Credit account tests
Write-Info "`n=== CREDIT ACCOUNT TESTS ==="

if ($script:testAccountNumber) {
    $creditBody = @{
        amount = 500.00
        referenceId = "REF-CREDIT-$(Get-Random)"
        description = "Test credit operation"
    } | ConvertTo-Json

    $creditResponse = Test-Endpoint `
        -Name "Credit Account" `
        -Method "POST" `
        -Url "$baseUrl/$script:testAccountNumber/credit" `
        -Body $creditBody `
        -Token $script:jwtToken `
        -ExpectedStatus 200 `
        -Description "Add funds to account (1000 + 500 = 1500)"

    if ($creditResponse -and $creditResponse.data.balance) {
        Write-Info "New Balance: $($creditResponse.data.balance) TRY"
    }
}

$invalidCreditBody = @{
    amount = -100.00
    description = "Invalid negative amount"
} | ConvertTo-Json

if ($script:testAccountNumber) {
    Test-Endpoint `
        -Name "Credit With Invalid Amount" `
        -Method "POST" `
        -Url "$baseUrl/$script:testAccountNumber/credit" `
        -Body $invalidCreditBody `
        -Token $script:jwtToken `
        -ExpectedStatus 400 `
        -Description "Should reject negative credit amount"
}

# Step 8: Debit account tests
Write-Info "`n=== DEBIT ACCOUNT TESTS ==="

if ($script:testAccountNumber) {
    $debitBody = @{
        amount = 300.00
        referenceId = "REF-DEBIT-$(Get-Random)"
        description = "Test debit operation"
    } | ConvertTo-Json

    $debitResponse = Test-Endpoint `
        -Name "Debit Account" `
        -Method "POST" `
        -Url "$baseUrl/$script:testAccountNumber/debit" `
        -Body $debitBody `
        -Token $script:jwtToken `
        -ExpectedStatus 200 `
        -Description "Withdraw funds from account (1500 - 300 = 1200)"

    if ($debitResponse -and $debitResponse.data.balance) {
        Write-Info "New Balance: $($debitResponse.data.balance) TRY"
    }
}

if ($script:testAccountNumber) {
    $excessiveDebitBody = @{
        amount = 999999.00
        description = "Insufficient balance test"
    } | ConvertTo-Json

    Test-Endpoint `
        -Name "Debit With Insufficient Balance" `
        -Method "POST" `
        -Url "$baseUrl/$script:testAccountNumber/debit" `
        -Body $excessiveDebitBody `
        -Token $script:jwtToken `
        -ExpectedStatus 400 `
        -Description "Should reject debit exceeding balance"
}

# Step 9: Freeze account tests
Write-Info "`n=== FREEZE ACCOUNT TESTS ==="

if ($script:testAccountNumber2) {
    $freezeResponse = Test-Endpoint `
        -Name "Freeze Account" `
        -Method "POST" `
        -Url "$baseUrl/$script:testAccountNumber2/freeze" `
        -Token $script:jwtToken `
        -ExpectedStatus 200 `
        -Description "Freeze account (ADMIN only operation)"

    if ($freezeResponse -and $freezeResponse.data.status) {
        Write-Info "Account Status: $($freezeResponse.data.status)"
    }
}

# Step 10: Test operations on frozen account
Write-Info "`n=== FROZEN ACCOUNT OPERATION TESTS ==="

if ($script:testAccountNumber2) {
    $creditFrozenBody = @{
        amount = 100.00
        description = "Test credit on frozen account"
    } | ConvertTo-Json

    Test-Endpoint `
        -Name "Credit Frozen Account" `
        -Method "POST" `
        -Url "$baseUrl/$script:testAccountNumber2/credit" `
        -Body $creditFrozenBody `
        -Token $script:jwtToken `
        -ExpectedStatus 400 `
        -Description "Should reject credit operation on frozen account"
}

# Step 11: Activate account tests
Write-Info "`n=== ACTIVATE ACCOUNT TESTS ==="

if ($script:testAccountNumber2) {
    $activateResponse = Test-Endpoint `
        -Name "Activate Account" `
        -Method "POST" `
        -Url "$baseUrl/$script:testAccountNumber2/activate" `
        -Token $script:jwtToken `
        -ExpectedStatus 200 `
        -Description "Reactivate previously frozen account"

    if ($activateResponse -and $activateResponse.data.status) {
        Write-Info "Account Status: $($activateResponse.data.status)"
    }
}

# Step 12: Get account history
Write-Info "`n=== ACCOUNT HISTORY TESTS ==="

if ($script:testAccountNumber) {
    $historyResponse = Test-Endpoint `
        -Name "Get Account History" `
        -Method "GET" `
        -Url "$baseUrl/$script:testAccountNumber/history" `
        -Token $script:jwtToken `
        -ExpectedStatus 200 `
        -Description "Retrieve complete audit trail for account"

    if ($historyResponse -and $historyResponse.data) {
        Write-Info "History Records: $($historyResponse.data.Count)"
        if ($historyResponse.data.Count -gt 0) {
            Write-Info "Recent Operations:"
            $historyResponse.data | Select-Object -First 3 | ForEach-Object {
                Write-Host "  - $($_.operation): $($_.description)" -ForegroundColor Gray
            }
        }
    }
}

# Step 13: Close account tests
Write-Info "`n=== CLOSE ACCOUNT TESTS ==="

# Create account with zero balance for closing
$closeAccountBody = @{
    customerId = "CUS-$(Get-Random -Minimum 100000 -Maximum 999999)"
    customerName = "To Be Closed"
    currency = "EUR"
    accountType = "CHECKING"
    initialBalance = 0.00
} | ConvertTo-Json

$closeAccountResponse = Test-Endpoint `
    -Name "Create Account For Closing" `
    -Method "POST" `
    -Url "$baseUrl" `
    -Body $closeAccountBody `
    -Token $script:jwtToken `
    -ExpectedStatus 201 `
    -Description "Create account with zero balance for closure test"

if ($closeAccountResponse -and $closeAccountResponse.data.accountNumber) {
    $accountToClose = $closeAccountResponse.data.accountNumber

    $closeResponse = Test-Endpoint `
        -Name "Close Account With Zero Balance" `
        -Method "POST" `
        -Url "$baseUrl/$accountToClose/close" `
        -Token $script:jwtToken `
        -ExpectedStatus 200 `
        -Description "Close account with zero balance (ADMIN only)"

    if ($closeResponse -and $closeResponse.data.status) {
        Write-Info "Account Status: $($closeResponse.data.status)"
    }
}

if ($script:testAccountNumber) {
    Test-Endpoint `
        -Name "Close Account With Non-Zero Balance" `
        -Method "POST" `
        -Url "$baseUrl/$script:testAccountNumber/close" `
        -Token $script:jwtToken `
        -ExpectedStatus 400 `
        -Description "Should reject closing account with non-zero balance"
}

# Step 14: Edge cases
Write-Info "`n=== EDGE CASE TESTS ==="

Test-Endpoint `
    -Name "Get Account With Invalid ID Format" `
    -Method "GET" `
    -Url "$baseUrl/invalid-id" `
    -Token $script:jwtToken `
    -ExpectedStatus 400 `
    -Description "Should return 400 for non-numeric account ID"

Test-Endpoint `
    -Name "Get Account With Invalid IBAN" `
    -Method "GET" `
    -Url "$baseUrl/number/INVALID-IBAN" `
    -Token $script:jwtToken `
    -ExpectedStatus 404 `
    -Description "Should return 404 for invalid IBAN format"

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
    exit 0
} else {
    Write-Error "`n✗ SOME TESTS FAILED ✗"
    exit 1
}
