# End-to-End Platform Integration Test
# Tests complete JWT authentication flow across all services
# Author: Claude Code
# Date: 2025-12-28

# Test Configuration
$BASE_URL = "http://localhost"
$AUTH_SERVICE = "${BASE_URL}:8084"
$ACCOUNT_SERVICE = "${BASE_URL}:8081"
$TRANSFER_SERVICE = "${BASE_URL}:8082"
$CUSTOMER_SERVICE = "${BASE_URL}:8083"

# Generate random test data
$RANDOM_NUMBER = Get-Random -Minimum 10000 -Maximum 99999
$TEST_EMAIL = "integration_test_${RANDOM_NUMBER}@example.com"
$TEST_PASSWORD = "Test@Password123"
$TEST_FIRST_NAME = "Integration"
$TEST_LAST_NAME = "Test${RANDOM_NUMBER}"

# Color output helpers
function Write-Success { param($message) Write-Host "✓ $message" -ForegroundColor Green }
function Write-Failure { param($message) Write-Host "✗ $message" -ForegroundColor Red; exit 1 }
function Write-Info { param($message) Write-Host "ℹ $message" -ForegroundColor Cyan }
function Write-Test { param($message) Write-Host "→ $message" -ForegroundColor Yellow }

# Global variables for test data
$ACCESS_TOKEN = ""
$REFRESH_TOKEN = ""
$CUSTOMER_ID = ""
$ACCOUNT_NUMBER = ""
$TRANSFER_REFERENCE = ""

Write-Host "`n==================================================" -ForegroundColor Magenta
Write-Host "  Banking Platform - Integration Test Suite" -ForegroundColor Magenta
Write-Host "==================================================" -ForegroundColor Magenta
Write-Host "Test User: $TEST_EMAIL" -ForegroundColor Gray
Write-Host "`n"

# ===========================
# Scenario 1: Auth Service - Register
# ===========================
Write-Test "Scenario 1: Register New User"
$registerBody = @{
    email = $TEST_EMAIL
    password = $TEST_PASSWORD
    firstName = $TEST_FIRST_NAME
    lastName = $TEST_LAST_NAME
} | ConvertTo-Json

try {
    $registerResponse = Invoke-RestMethod -Uri "$AUTH_SERVICE/auth/register" `
        -Method Post `
        -Body $registerBody `
        -ContentType "application/json"

    if ($registerResponse.success) {
        $ACCESS_TOKEN = $registerResponse.data.accessToken
        $REFRESH_TOKEN = $registerResponse.data.refreshToken
        Write-Success "User registered successfully with JWT tokens"
        Write-Info "  Access Token: $($ACCESS_TOKEN.Substring(0, 20))..."
    } else {
        Write-Failure "Registration failed: $($registerResponse.message)"
    }
} catch {
    Write-Failure "Registration request failed: $_"
}

# ===========================
# Scenario 2: Auth Service - Get Profile
# ===========================
Write-Test "Scenario 2: Get User Profile with JWT"
try {
    $headers = @{
        "Authorization" = "Bearer $ACCESS_TOKEN"
    }

    $profileResponse = Invoke-RestMethod -Uri "$AUTH_SERVICE/auth/me" `
        -Method Get `
        -Headers $headers

    if ($profileResponse.success) {
        Write-Success "Profile retrieved successfully"
        Write-Info "  User ID: $($profileResponse.data.userId)"
        Write-Info "  Email: $($profileResponse.data.email)"
        Write-Info "  Roles: $($profileResponse.data.roles -join ', ')"
    } else {
        Write-Failure "Get profile failed: $($profileResponse.message)"
    }
} catch {
    Write-Failure "Get profile request failed: $_"
}

# ===========================
# Scenario 3: Customer Service - Create Customer (JWT Protected)
# ===========================
Write-Test "Scenario 3: Create Customer with JWT Authentication"
$customerBody = @{
    firstName = $TEST_FIRST_NAME
    lastName = $TEST_LAST_NAME
    email = $TEST_EMAIL
    nationalId = "12345678901"
    phoneNumber = "+905551234567"
    address = "Test Address 123"
    city = "Istanbul"
    country = "Turkey"
} | ConvertTo-Json

try {
    $headers = @{
        "Authorization" = "Bearer $ACCESS_TOKEN"
    }

    $customerResponse = Invoke-RestMethod -Uri "$CUSTOMER_SERVICE/customers" `
        -Method Post `
        -Headers $headers `
        -Body $customerBody `
        -ContentType "application/json"

    $CUSTOMER_ID = $customerResponse.customerId
    Write-Success "Customer created successfully"
    Write-Info "  Customer ID: $CUSTOMER_ID"
} catch {
    Write-Failure "Create customer request failed: $_"
}

# ===========================
# Scenario 4: Account Service - Create Account (JWT Protected)
# ===========================
Write-Test "Scenario 4: Create Account with JWT Authentication"
$accountBody = @{
    customerName = "$TEST_FIRST_NAME $TEST_LAST_NAME"
    accountType = "CHECKING"
    currency = "TRY"
    initialBalance = 1000.00
} | ConvertTo-Json

try {
    $headers = @{
        "Authorization" = "Bearer $ACCESS_TOKEN"
    }

    $accountResponse = Invoke-RestMethod -Uri "$ACCOUNT_SERVICE/accounts" `
        -Method Post `
        -Headers $headers `
        -Body $accountBody `
        -ContentType "application/json"

    $ACCOUNT_NUMBER = $accountResponse.accountNumber
    Write-Success "Account created successfully"
    Write-Info "  Account Number: $ACCOUNT_NUMBER"
    Write-Info "  IBAN: $($accountResponse.iban)"
    Write-Info "  Balance: $($accountResponse.balance) $($accountResponse.currency)"
} catch {
    Write-Failure "Create account request failed: $_"
}

# ===========================
# Scenario 5: Account Service - Get Account Details (JWT Protected)
# ===========================
Write-Test "Scenario 5: Get Account Details with JWT"
try {
    $headers = @{
        "Authorization" = "Bearer $ACCESS_TOKEN"
    }

    $accountDetails = Invoke-RestMethod -Uri "$ACCOUNT_SERVICE/accounts/$ACCOUNT_NUMBER" `
        -Method Get `
        -Headers $headers

    Write-Success "Account details retrieved successfully"
    Write-Info "  Balance: $($accountDetails.balance) $($accountDetails.currency)"
    Write-Info "  Status: $($accountDetails.status)"
} catch {
    Write-Failure "Get account details request failed: $_"
}

# ===========================
# Scenario 6: Transfer Service - Create Transfer (JWT Protected)
# ===========================
# First create a second account for transfer
Write-Test "Scenario 6a: Create Second Account for Transfer"
$targetAccountBody = @{
    customerName = "Target Customer"
    accountType = "SAVINGS"
    currency = "TRY"
    initialBalance = 500.00
} | ConvertTo-Json

try {
    $headers = @{
        "Authorization" = "Bearer $ACCESS_TOKEN"
    }

    $targetAccountResponse = Invoke-RestMethod -Uri "$ACCOUNT_SERVICE/accounts" `
        -Method Post `
        -Headers $headers `
        -Body $targetAccountBody `
        -ContentType "application/json"

    $TARGET_ACCOUNT = $targetAccountResponse.accountNumber
    Write-Success "Target account created: $TARGET_ACCOUNT"
} catch {
    Write-Failure "Create target account failed: $_"
}

Write-Test "Scenario 6b: Execute Transfer with JWT"
$transferBody = @{
    fromAccountNumber = $ACCOUNT_NUMBER
    toAccountNumber = $TARGET_ACCOUNT
    amount = 250.00
    currency = "TRY"
    idempotencyKey = "test-transfer-$(Get-Random)"
} | ConvertTo-Json

try {
    $headers = @{
        "Authorization" = "Bearer $ACCESS_TOKEN"
    }

    $transferResponse = Invoke-RestMethod -Uri "$TRANSFER_SERVICE/transfers" `
        -Method Post `
        -Headers $headers `
        -Body $transferBody `
        -ContentType "application/json"

    $TRANSFER_REFERENCE = $transferResponse.transferReference
    Write-Success "Transfer executed successfully"
    Write-Info "  Transfer Reference: $TRANSFER_REFERENCE"
    Write-Info "  Amount: $($transferResponse.amount) $($transferResponse.currency)"
    Write-Info "  Status: $($transferResponse.status)"
} catch {
    Write-Failure "Transfer request failed: $_"
}

# ===========================
# Scenario 7: Transfer Service - Get Transfer Status (JWT Protected)
# ===========================
Write-Test "Scenario 7: Get Transfer Status with JWT"
try {
    $headers = @{
        "Authorization" = "Bearer $ACCESS_TOKEN"
    }

    $transferStatus = Invoke-RestMethod -Uri "$TRANSFER_SERVICE/transfers/$TRANSFER_REFERENCE" `
        -Method Get `
        -Headers $headers

    Write-Success "Transfer status retrieved successfully"
    Write-Info "  Status: $($transferStatus.status)"
    Write-Info "  Debit TX: $($transferStatus.debitTransactionId)"
    Write-Info "  Credit TX: $($transferStatus.creditTransactionId)"
} catch {
    Write-Failure "Get transfer status request failed: $_"
}

# ===========================
# Scenario 8: Auth Service - Refresh Token
# ===========================
Write-Test "Scenario 8: Refresh Access Token"
$refreshBody = @{
    refreshToken = $REFRESH_TOKEN
} | ConvertTo-Json

try {
    $refreshResponse = Invoke-RestMethod -Uri "$AUTH_SERVICE/auth/refresh" `
        -Method Post `
        -Body $refreshBody `
        -ContentType "application/json"

    if ($refreshResponse.success) {
        $OLD_ACCESS_TOKEN = $ACCESS_TOKEN
        $ACCESS_TOKEN = $refreshResponse.data.accessToken
        $REFRESH_TOKEN = $refreshResponse.data.refreshToken
        Write-Success "Tokens refreshed successfully"
        Write-Info "  New Access Token: $($ACCESS_TOKEN.Substring(0, 20))..."
        Write-Info "  Old token blacklisted: Yes"
    } else {
        Write-Failure "Token refresh failed: $($refreshResponse.message)"
    }
} catch {
    Write-Failure "Token refresh request failed: $_"
}

# ===========================
# Scenario 9: Verify Old Token is Blacklisted
# ===========================
Write-Test "Scenario 9: Verify Old Token Cannot Be Used"
try {
    $headers = @{
        "Authorization" = "Bearer $OLD_ACCESS_TOKEN"
    }

    $accountResponse = Invoke-RestMethod -Uri "$ACCOUNT_SERVICE/accounts/$ACCOUNT_NUMBER" `
        -Method Get `
        -Headers $headers `
        -ErrorAction Stop

    Write-Failure "Old token should have been rejected (blacklisted)"
} catch {
    if ($_.Exception.Response.StatusCode -eq 401) {
        Write-Success "Old token correctly rejected (401 Unauthorized)"
    } else {
        Write-Failure "Unexpected error: $_"
    }
}

# ===========================
# Scenario 10: Verify New Token Works
# ===========================
Write-Test "Scenario 10: Verify New Token Works"
try {
    $headers = @{
        "Authorization" = "Bearer $ACCESS_TOKEN"
    }

    $accountResponse = Invoke-RestMethod -Uri "$ACCOUNT_SERVICE/accounts/$ACCOUNT_NUMBER" `
        -Method Get `
        -Headers $headers

    Write-Success "New token works correctly"
} catch {
    Write-Failure "New token request failed: $_"
}

# ===========================
# Scenario 11: Auth Service - Logout
# ===========================
Write-Test "Scenario 11: Logout and Blacklist Token"
try {
    $headers = @{
        "Authorization" = "Bearer $ACCESS_TOKEN"
    }

    $logoutResponse = Invoke-RestMethod -Uri "$AUTH_SERVICE/auth/logout" `
        -Method Post `
        -Headers $headers

    if ($logoutResponse.success) {
        Write-Success "Logged out successfully, token blacklisted"
    } else {
        Write-Failure "Logout failed: $($logoutResponse.message)"
    }
} catch {
    Write-Failure "Logout request failed: $_"
}

# ===========================
# Scenario 12: Verify Blacklisted Token Cannot Be Used
# ===========================
Write-Test "Scenario 12: Verify Blacklisted Token is Rejected"
try {
    $headers = @{
        "Authorization" = "Bearer $ACCESS_TOKEN"
    }

    $accountResponse = Invoke-RestMethod -Uri "$ACCOUNT_SERVICE/accounts/$ACCOUNT_NUMBER" `
        -Method Get `
        -Headers $headers `
        -ErrorAction Stop

    Write-Failure "Blacklisted token should have been rejected"
} catch {
    if ($_.Exception.Response.StatusCode -eq 401) {
        Write-Success "Blacklisted token correctly rejected (401 Unauthorized)"
    } else {
        Write-Failure "Unexpected error: $_"
    }
}

# ===========================
# Scenario 13: Verify Protected Endpoints Without Token
# ===========================
Write-Test "Scenario 13: Verify Endpoints Require Authentication"
try {
    $accountResponse = Invoke-RestMethod -Uri "$ACCOUNT_SERVICE/accounts" `
        -Method Get `
        -ErrorAction Stop

    Write-Failure "Request without JWT should have been rejected"
} catch {
    if ($_.Exception.Response.StatusCode -eq 401) {
        Write-Success "Request without JWT correctly rejected (401 Unauthorized)"
    } else {
        Write-Failure "Unexpected error: $_"
    }
}

# ===========================
# TEST SUMMARY
# ===========================
Write-Host "`n==================================================" -ForegroundColor Magenta
Write-Host "  Integration Test Summary" -ForegroundColor Magenta
Write-Host "==================================================" -ForegroundColor Magenta
Write-Success "All 13 integration test scenarios passed!"
Write-Host "`nTest Coverage:" -ForegroundColor Cyan
Write-Host "  ✓ User registration with JWT" -ForegroundColor White
Write-Host "  ✓ User profile retrieval" -ForegroundColor White
Write-Host "  ✓ Customer creation (JWT protected)" -ForegroundColor White
Write-Host "  ✓ Account creation (JWT protected)" -ForegroundColor White
Write-Host "  ✓ Account details retrieval (JWT protected)" -ForegroundColor White
Write-Host "  ✓ Money transfer execution (JWT protected)" -ForegroundColor White
Write-Host "  ✓ Transfer status retrieval (JWT protected)" -ForegroundColor White
Write-Host "  ✓ Token refresh flow" -ForegroundColor White
Write-Host "  ✓ Old token blacklisting verification" -ForegroundColor White
Write-Host "  ✓ New token validation" -ForegroundColor White
Write-Host "  ✓ Logout and token blacklisting" -ForegroundColor White
Write-Host "  ✓ Blacklisted token rejection" -ForegroundColor White
Write-Host "  ✓ Protected endpoints authentication requirement" -ForegroundColor White
Write-Host "`nTest Data:" -ForegroundColor Cyan
Write-Host "  User: $TEST_EMAIL" -ForegroundColor Gray
Write-Host "  Customer ID: $CUSTOMER_ID" -ForegroundColor Gray
Write-Host "  Account Number: $ACCOUNT_NUMBER" -ForegroundColor Gray
Write-Host "  Transfer Reference: $TRANSFER_REFERENCE" -ForegroundColor Gray
Write-Host "`n✓ Banking Platform Integration: COMPLETE" -ForegroundColor Green
Write-Host "==================================================" -ForegroundColor Magenta
Write-Host "`n"

exit 0
