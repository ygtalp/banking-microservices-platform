# Test Authentication Service
# Tests all authentication endpoints including register, login, logout, and refresh

param(
    [string]$BaseUrl = "http://localhost:8084"
)

$ErrorActionPreference = "Continue"
$testsPassed = 0
$testsFailed = 0

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Authentication Service API Tests" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Base URL: $BaseUrl" -ForegroundColor Yellow
Write-Host ""

function Test-Endpoint {
    param(
        [string]$Name,
        [scriptblock]$Test
    )

    Write-Host "Testing: $Name" -ForegroundColor Yellow
    try {
        & $Test
        Write-Host "✓ PASSED: $Name" -ForegroundColor Green
        $script:testsPassed++
    }
    catch {
        Write-Host "✗ FAILED: $Name" -ForegroundColor Red
        Write-Host "  Error: $_" -ForegroundColor Red
        $script:testsFailed++
    }
    Write-Host ""
}

# Test 1: Health Check
Test-Endpoint "Health Check" {
    $response = Invoke-RestMethod -Uri "$BaseUrl/auth/health" -Method GET
    if ($response.success -ne $true) {
        throw "Health check failed"
    }
    Write-Host "  Status: $($response.data)" -ForegroundColor Gray
}

# Test 2: Register New User
$randomEmail = "test$(Get-Random -Minimum 1000 -Maximum 9999)@example.com"
$registerResponse = $null

Test-Endpoint "Register New User" {
    $registerBody = @{
        email = $randomEmail
        password = "Test@1234"
        firstName = "Test"
        lastName = "User"
        phoneNumber = "+905551234567"
    } | ConvertTo-Json

    $script:registerResponse = Invoke-RestMethod -Uri "$BaseUrl/auth/register" `
        -Method POST `
        -Body $registerBody `
        -ContentType "application/json"

    if ($script:registerResponse.success -ne $true) {
        throw "Registration failed"
    }

    Write-Host "  User ID: $($script:registerResponse.data.userId)" -ForegroundColor Gray
    Write-Host "  Email: $($script:registerResponse.data.email)" -ForegroundColor Gray
    Write-Host "  Roles: $($script:registerResponse.data.roles -join ', ')" -ForegroundColor Gray
    Write-Host "  Access Token: $($script:registerResponse.data.accessToken.Substring(0, 20))..." -ForegroundColor Gray
}

# Test 3: Login with Created User
$loginResponse = $null

Test-Endpoint "Login with Valid Credentials" {
    $loginBody = @{
        email = $randomEmail
        password = "Test@1234"
    } | ConvertTo-Json

    $script:loginResponse = Invoke-RestMethod -Uri "$BaseUrl/auth/login" `
        -Method POST `
        -Body $loginBody `
        -ContentType "application/json"

    if ($script:loginResponse.success -ne $true) {
        throw "Login failed"
    }

    Write-Host "  Access Token: $($script:loginResponse.data.accessToken.Substring(0, 20))..." -ForegroundColor Gray
    Write-Host "  Refresh Token: $($script:loginResponse.data.refreshToken.Substring(0, 20))..." -ForegroundColor Gray
}

# Test 4: Get Current User Profile
Test-Endpoint "Get Current User Profile" {
    $headers = @{
        Authorization = "Bearer $($loginResponse.data.accessToken)"
    }

    $profileResponse = Invoke-RestMethod -Uri "$BaseUrl/auth/me" `
        -Method GET `
        -Headers $headers

    if ($profileResponse.success -ne $true) {
        throw "Get profile failed"
    }

    if ($profileResponse.data.email -ne $randomEmail) {
        throw "Email mismatch"
    }

    Write-Host "  User ID: $($profileResponse.data.userId)" -ForegroundColor Gray
    Write-Host "  Name: $($profileResponse.data.firstName) $($profileResponse.data.lastName)" -ForegroundColor Gray
    Write-Host "  Status: $($profileResponse.data.status)" -ForegroundColor Gray
}

# Test 5: Change Password
Test-Endpoint "Change Password" {
    $headers = @{
        Authorization = "Bearer $($loginResponse.data.accessToken)"
    }

    $changePasswordBody = @{
        currentPassword = "Test@1234"
        newPassword = "NewTest@5678"
    } | ConvertTo-Json

    $changePasswordResponse = Invoke-RestMethod -Uri "$BaseUrl/auth/password/change" `
        -Method POST `
        -Headers $headers `
        -Body $changePasswordBody `
        -ContentType "application/json"

    if ($changePasswordResponse.success -ne $true) {
        throw "Change password failed"
    }

    Write-Host "  Password changed successfully" -ForegroundColor Gray
}

# Test 6: Login with New Password
Test-Endpoint "Login with New Password" {
    $loginBody = @{
        email = $randomEmail
        password = "NewTest@5678"
    } | ConvertTo-Json

    $newLoginResponse = Invoke-RestMethod -Uri "$BaseUrl/auth/login" `
        -Method POST `
        -Body $loginBody `
        -ContentType "application/json"

    if ($newLoginResponse.success -ne $true) {
        throw "Login with new password failed"
    }

    # Update login response for next tests
    $script:loginResponse = $newLoginResponse

    Write-Host "  Login successful with new password" -ForegroundColor Gray
}

# Test 7: Refresh Token
$refreshResponse = $null

Test-Endpoint "Refresh Access Token" {
    $refreshBody = @{
        refreshToken = $loginResponse.data.refreshToken
    } | ConvertTo-Json

    $script:refreshResponse = Invoke-RestMethod -Uri "$BaseUrl/auth/refresh" `
        -Method POST `
        -Body $refreshBody `
        -ContentType "application/json"

    if ($script:refreshResponse.success -ne $true) {
        throw "Token refresh failed"
    }

    Write-Host "  New Access Token: $($script:refreshResponse.data.accessToken.Substring(0, 20))..." -ForegroundColor Gray
    Write-Host "  New Refresh Token: $($script:refreshResponse.data.refreshToken.Substring(0, 20))..." -ForegroundColor Gray
}

# Test 8: Logout
Test-Endpoint "Logout" {
    $headers = @{
        Authorization = "Bearer $($refreshResponse.data.accessToken)"
    }

    $logoutResponse = Invoke-RestMethod -Uri "$BaseUrl/auth/logout" `
        -Method POST `
        -Headers $headers

    if ($logoutResponse.success -ne $true) {
        throw "Logout failed"
    }

    Write-Host "  Logout successful" -ForegroundColor Gray
}

# Test 9: Try to use logged out token (should fail)
Test-Endpoint "Verify Token Blacklisted After Logout" {
    $headers = @{
        Authorization = "Bearer $($refreshResponse.data.accessToken)"
    }

    try {
        $profileResponse = Invoke-RestMethod -Uri "$BaseUrl/auth/me" `
            -Method GET `
            -Headers $headers

        # If we got here, the token wasn't blacklisted
        throw "Token should have been blacklisted but request succeeded"
    }
    catch {
        # Expected to fail with 401
        if ($_.Exception.Response.StatusCode.value__ -ne 401) {
            throw "Expected 401 Unauthorized but got $($_.Exception.Response.StatusCode.value__)"
        }
        Write-Host "  Token correctly blacklisted (401 Unauthorized)" -ForegroundColor Gray
    }
}

# Test 10: Invalid Credentials
Test-Endpoint "Login with Invalid Credentials" {
    $loginBody = @{
        email = "invalid@example.com"
        password = "WrongPassword"
    } | ConvertTo-Json

    try {
        $response = Invoke-RestMethod -Uri "$BaseUrl/auth/login" `
            -Method POST `
            -Body $loginBody `
            -ContentType "application/json"

        throw "Login should have failed but succeeded"
    }
    catch {
        # Expected to fail with 401
        if ($_.Exception.Response.StatusCode.value__ -ne 401) {
            throw "Expected 401 but got $($_.Exception.Response.StatusCode.value__)"
        }
        Write-Host "  Correctly rejected invalid credentials (401)" -ForegroundColor Gray
    }
}

# Test 11: Duplicate Email Registration
Test-Endpoint "Prevent Duplicate Email Registration" {
    $registerBody = @{
        email = $randomEmail  # Same email as before
        password = "Test@1234"
        firstName = "Duplicate"
        lastName = "User"
    } | ConvertTo-Json

    try {
        $response = Invoke-RestMethod -Uri "$BaseUrl/auth/register" `
            -Method POST `
            -Body $registerBody `
            -ContentType "application/json"

        throw "Registration should have failed for duplicate email"
    }
    catch {
        # Expected to fail with 409 Conflict
        if ($_.Exception.Response.StatusCode.value__ -ne 409) {
            throw "Expected 409 Conflict but got $($_.Exception.Response.StatusCode.value__)"
        }
        Write-Host "  Correctly rejected duplicate email (409 Conflict)" -ForegroundColor Gray
    }
}

# Test 12: Weak Password Validation
Test-Endpoint "Password Validation" {
    $weakEmail = "weak$(Get-Random)@example.com"
    $registerBody = @{
        email = $weakEmail
        password = "weak"  # Too weak
        firstName = "Weak"
        lastName = "Pass"
    } | ConvertTo-Json

    try {
        $response = Invoke-RestMethod -Uri "$BaseUrl/auth/register" `
            -Method POST `
            -Body $registerBody `
            -ContentType "application/json"

        throw "Registration should have failed for weak password"
    }
    catch {
        # Expected to fail with 400 Bad Request
        if ($_.Exception.Response.StatusCode.value__ -ne 400) {
            throw "Expected 400 Bad Request but got $($_.Exception.Response.StatusCode.value__)"
        }
        Write-Host "  Correctly rejected weak password (400 Bad Request)" -ForegroundColor Gray
    }
}

# Summary
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Test Summary" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Total Tests: $($testsPassed + $testsFailed)" -ForegroundColor Yellow
Write-Host "Passed: $testsPassed" -ForegroundColor Green
Write-Host "Failed: $testsFailed" -ForegroundColor $(if ($testsFailed -eq 0) { "Green" } else { "Red" })
Write-Host ""

if ($testsFailed -eq 0) {
    Write-Host "✓ All tests passed!" -ForegroundColor Green
    exit 0
} else {
    Write-Host "✗ Some tests failed" -ForegroundColor Red
    exit 1
}
