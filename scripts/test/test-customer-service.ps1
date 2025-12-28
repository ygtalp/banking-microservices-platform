# Test Customer Service API
# Comprehensive end-to-end tests for Customer Service

$ErrorActionPreference = "Stop"

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "   Customer Service API Tests" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

$baseUrl = "http://localhost:8080/api/v1/customers"
$docUrl = "http://localhost:8080/api/v1/documents"
$testsPassed = 0
$testsFailed = 0

function Test-Endpoint {
    param(
        [string]$TestName,
        [scriptblock]$TestScript
    )

    Write-Host "`n[$TestName]" -ForegroundColor Yellow
    try {
        & $TestScript
        Write-Host "  ✓ PASSED" -ForegroundColor Green
        $script:testsPassed++
    }
    catch {
        Write-Host "  ✗ FAILED: $($_.Exception.Message)" -ForegroundColor Red
        $script:testsFailed++
    }
}

# Test 1: Register Customer
Test-Endpoint "Test 1: Register Customer" {
    $registerRequest = @{
        firstName = "John"
        lastName = "Doe"
        email = "john.doe.test@banking.nl"
        phoneNumber = "+31612345678"
        dateOfBirth = "1990-01-15"
        nationalId = "12345678901"
        address = "Test Street 123"
        city = "Amsterdam"
        country = "Netherlands"
        postalCode = "1015 CJ"
    } | ConvertTo-Json

    $response = Invoke-RestMethod -Uri $baseUrl `
        -Method POST `
        -Body $registerRequest `
        -ContentType "application/json"

    if ($response.success -ne $true) {
        throw "Registration failed"
    }

    $script:customerId = $response.data.customerId
    $script:customerInternalId = $response.data.id

    Write-Host "  Customer ID: $($script:customerId)" -ForegroundColor Gray
    Write-Host "  Status: $($response.data.status)" -ForegroundColor Gray
    Write-Host "  National ID (masked): $($response.data.nationalId)" -ForegroundColor Gray

    if ($response.data.status -ne "PENDING_VERIFICATION") {
        throw "Expected status PENDING_VERIFICATION, got $($response.data.status)"
    }

    if (-not $response.data.nationalId.Contains("***")) {
        throw "National ID should be masked"
    }
}

# Test 2: Get Customer by ID
Test-Endpoint "Test 2: Get Customer by ID" {
    $response = Invoke-RestMethod -Uri "$baseUrl/$($script:customerId)" -Method GET

    if ($response.success -ne $true) {
        throw "Failed to retrieve customer"
    }

    if ($response.data.customerId -ne $script:customerId) {
        throw "Customer ID mismatch"
    }

    Write-Host "  Retrieved: $($response.data.firstName) $($response.data.lastName)" -ForegroundColor Gray
}

# Test 3: Get Customer by Email
Test-Endpoint "Test 3: Get Customer by Email" {
    $email = "john.doe.test@banking.nl"
    $response = Invoke-RestMethod -Uri "$baseUrl/email/$email" -Method GET

    if ($response.success -ne $true) {
        throw "Failed to retrieve customer by email"
    }

    if ($response.data.email -ne $email) {
        throw "Email mismatch"
    }

    Write-Host "  Found by email: $email" -ForegroundColor Gray
}

# Test 4: Get Customer by National ID
Test-Endpoint "Test 4: Get Customer by National ID" {
    $nationalId = "12345678901"
    $response = Invoke-RestMethod -Uri "$baseUrl/national-id/$nationalId" -Method GET

    if ($response.success -ne $true) {
        throw "Failed to retrieve customer by national ID"
    }

    Write-Host "  Found by national ID" -ForegroundColor Gray
}

# Test 5: Upload KYC Document - Passport
Test-Endpoint "Test 5: Upload KYC Document - Passport" {
    $uploadRequest = @{
        customerId = $script:customerId
        documentType = "PASSPORT"
        documentNumber = "P12345678"
        issuingAuthority = "Netherlands Government"
        issueDate = "2020-01-01"
        expiryDate = "2030-01-01"
        documentUrl = "/documents/passport_john_doe.pdf"
    } | ConvertTo-Json

    $response = Invoke-RestMethod -Uri $docUrl `
        -Method POST `
        -Body $uploadRequest `
        -ContentType "application/json"

    if ($response.success -ne $true) {
        throw "Document upload failed"
    }

    $script:passportDocId = $response.data.id

    Write-Host "  Document ID: $($script:passportDocId)" -ForegroundColor Gray
    Write-Host "  Type: $($response.data.documentType)" -ForegroundColor Gray
    Write-Host "  Status: $($response.data.status)" -ForegroundColor Gray
}

# Test 6: Upload KYC Document - National ID
Test-Endpoint "Test 6: Upload KYC Document - National ID" {
    $uploadRequest = @{
        customerId = $script:customerId
        documentType = "NATIONAL_ID"
        documentNumber = "NID123456789"
        issuingAuthority = "Dutch Immigration Service"
        issueDate = "2021-06-15"
        expiryDate = "2031-06-15"
        documentUrl = "/documents/national_id_john_doe.pdf"
    } | ConvertTo-Json

    $response = Invoke-RestMethod -Uri $docUrl `
        -Method POST `
        -Body $uploadRequest `
        -ContentType "application/json"

    if ($response.success -ne $true) {
        throw "Document upload failed"
    }

    $script:nationalIdDocId = $response.data.id

    Write-Host "  Document ID: $($script:nationalIdDocId)" -ForegroundColor Gray
}

# Test 7: List Customer Documents
Test-Endpoint "Test 7: List Customer Documents" {
    $response = Invoke-RestMethod -Uri "$docUrl/customer/$($script:customerId)" -Method GET

    if ($response.success -ne $true) {
        throw "Failed to list documents"
    }

    if ($response.data.Count -lt 2) {
        throw "Expected at least 2 documents, got $($response.data.Count)"
    }

    Write-Host "  Total documents: $($response.data.Count)" -ForegroundColor Gray
}

# Test 8: Verify KYC Document
Test-Endpoint "Test 8: Verify KYC Document" {
    $verifyRequest = @{
        verifiedBy = "admin@banking.nl"
    } | ConvertTo-Json

    $response = Invoke-RestMethod -Uri "$docUrl/$($script:passportDocId)/verify" `
        -Method POST `
        -Body $verifyRequest `
        -ContentType "application/json"

    if ($response.success -ne $true) {
        throw "Document verification failed"
    }

    if ($response.data.status -ne "VERIFIED") {
        throw "Expected status VERIFIED, got $($response.data.status)"
    }

    Write-Host "  Document verified by: $($response.data.verifiedBy)" -ForegroundColor Gray
}

# Test 9: Verify Second Document
Test-Endpoint "Test 9: Verify Second Document" {
    $verifyRequest = @{
        verifiedBy = "admin@banking.nl"
    } | ConvertTo-Json

    $response = Invoke-RestMethod -Uri "$docUrl/$($script:nationalIdDocId)/verify" `
        -Method POST `
        -Body $verifyRequest `
        -ContentType "application/json"

    if ($response.success -ne $true) {
        throw "Document verification failed"
    }
}

# Test 10: Verify Customer
Test-Endpoint "Test 10: Verify Customer" {
    $verifyRequest = @{
        verifiedBy = "admin@banking.nl"
    } | ConvertTo-Json

    $response = Invoke-RestMethod -Uri "$baseUrl/$($script:customerId)/verify" `
        -Method POST `
        -Body $verifyRequest `
        -ContentType "application/json"

    if ($response.success -ne $true) {
        throw "Customer verification failed"
    }

    if ($response.data.status -ne "VERIFIED") {
        throw "Expected status VERIFIED, got $($response.data.status)"
    }

    Write-Host "  Customer verified by: $($response.data.verifiedBy)" -ForegroundColor Gray
    Write-Host "  Verified at: $($response.data.verifiedAt)" -ForegroundColor Gray
}

# Test 11: Update Customer Information
Test-Endpoint "Test 11: Update Customer Information" {
    $updateRequest = @{
        phoneNumber = "+31687654321"
        address = "New Avenue 456"
        city = "Rotterdam"
        postalCode = "3011 AD"
    } | ConvertTo-Json

    $response = Invoke-RestMethod -Uri "$baseUrl/$($script:customerId)" `
        -Method PUT `
        -Body $updateRequest `
        -ContentType "application/json"

    if ($response.success -ne $true) {
        throw "Customer update failed"
    }

    if ($response.data.phoneNumber -ne "+31687654321") {
        throw "Phone number not updated"
    }

    Write-Host "  Updated phone: $($response.data.phoneNumber)" -ForegroundColor Gray
    Write-Host "  Updated address: $($response.data.address)" -ForegroundColor Gray
}

# Test 12: Approve Customer
Test-Endpoint "Test 12: Approve Customer" {
    $approveRequest = @{
        approvedBy = "manager@banking.nl"
        riskLevel = "LOW"
    } | ConvertTo-Json

    $response = Invoke-RestMethod -Uri "$baseUrl/$($script:customerId)/approve" `
        -Method POST `
        -Body $approveRequest `
        -ContentType "application/json"

    if ($response.success -ne $true) {
        throw "Customer approval failed"
    }

    if ($response.data.status -ne "APPROVED") {
        throw "Expected status APPROVED, got $($response.data.status)"
    }

    Write-Host "  Customer approved by: $($response.data.approvedBy)" -ForegroundColor Gray
    Write-Host "  Risk level: $($response.data.riskLevel)" -ForegroundColor Gray
}

# Test 13: Get Customer History
Test-Endpoint "Test 13: Get Customer History" {
    $response = Invoke-RestMethod -Uri "$baseUrl/$($script:customerId)/history" -Method GET

    if ($response.success -ne $true) {
        throw "Failed to retrieve customer history"
    }

    if ($response.data.Count -lt 4) {
        throw "Expected at least 4 history entries (register, verify, approve, update)"
    }

    Write-Host "  Total history entries: $($response.data.Count)" -ForegroundColor Gray
    foreach ($entry in $response.data) {
        Write-Host "    - $($entry.action): $($entry.description)" -ForegroundColor Gray
    }
}

# Test 14: Suspend Customer
Test-Endpoint "Test 14: Suspend Customer" {
    $suspendRequest = @{
        reason = "Suspicious activity detected for testing"
        suspendedBy = "compliance@banking.nl"
    } | ConvertTo-Json

    $response = Invoke-RestMethod -Uri "$baseUrl/$($script:customerId)/suspend" `
        -Method POST `
        -Body $suspendRequest `
        -ContentType "application/json"

    if ($response.success -ne $true) {
        throw "Customer suspension failed"
    }

    if ($response.data.status -ne "SUSPENDED") {
        throw "Expected status SUSPENDED, got $($response.data.status)"
    }

    Write-Host "  Status: $($response.data.status)" -ForegroundColor Gray
    Write-Host "  Reason: $($response.data.statusReason)" -ForegroundColor Gray
}

# Test 15: Reactivate Customer
Test-Endpoint "Test 15: Reactivate Customer" {
    $response = Invoke-RestMethod -Uri "$baseUrl/$($script:customerId)/activate" -Method POST

    if ($response.success -ne $true) {
        throw "Customer reactivation failed"
    }

    if ($response.data.status -ne "APPROVED") {
        throw "Expected status APPROVED after reactivation, got $($response.data.status)"
    }

    Write-Host "  Status: $($response.data.status)" -ForegroundColor Gray
}

# Test 16: Reject Document (Create new one first)
Test-Endpoint "Test 16: Reject Document" {
    # Upload a new document
    $uploadRequest = @{
        customerId = $script:customerId
        documentType = "PROOF_OF_ADDRESS"
        documentNumber = "UTIL-2024-001"
        issuingAuthority = "Energy Company NL"
        issueDate = "2024-01-01"
        expiryDate = "2025-01-01"
        documentUrl = "/documents/utility_bill.pdf"
    } | ConvertTo-Json

    $uploadResponse = Invoke-RestMethod -Uri $docUrl `
        -Method POST `
        -Body $uploadRequest `
        -ContentType "application/json"

    $docId = $uploadResponse.data.id

    # Reject the document
    $rejectRequest = @{
        rejectedBy = "admin@banking.nl"
        rejectionReason = "Document quality is too low - testing rejection flow"
    } | ConvertTo-Json

    $response = Invoke-RestMethod -Uri "$docUrl/$docId/reject" `
        -Method POST `
        -Body $rejectRequest `
        -ContentType "application/json"

    if ($response.success -ne $true) {
        throw "Document rejection failed"
    }

    if ($response.data.status -ne "REJECTED") {
        throw "Expected status REJECTED, got $($response.data.status)"
    }

    Write-Host "  Rejection reason: $($response.data.rejectionReason)" -ForegroundColor Gray
}

# Test 17: Error Handling - Duplicate Email
Test-Endpoint "Test 17: Error Handling - Duplicate Email" {
    $registerRequest = @{
        firstName = "Jane"
        lastName = "Smith"
        email = "john.doe.test@banking.nl"  # Same email as Test 1
        phoneNumber = "+31699887766"
        dateOfBirth = "1995-05-20"
        nationalId = "98765432109"
        address = "Other Street 789"
        city = "Utrecht"
        country = "Netherlands"
    } | ConvertTo-Json

    try {
        $response = Invoke-RestMethod -Uri $baseUrl `
            -Method POST `
            -Body $registerRequest `
            -ContentType "application/json"

        throw "Should have failed with duplicate email"
    }
    catch {
        if ($_.Exception.Response.StatusCode -ne 409) {
            throw "Expected 409 Conflict, got $($_.Exception.Response.StatusCode)"
        }
        Write-Host "  Correctly rejected duplicate email (409 Conflict)" -ForegroundColor Gray
    }
}

# Test 18: Error Handling - Customer Not Found
Test-Endpoint "Test 18: Error Handling - Customer Not Found" {
    try {
        $response = Invoke-RestMethod -Uri "$baseUrl/CUS-NOTEXIST" -Method GET
        throw "Should have failed with not found"
    }
    catch {
        if ($_.Exception.Response.StatusCode -ne 404) {
            throw "Expected 404 Not Found, got $($_.Exception.Response.StatusCode)"
        }
        Write-Host "  Correctly returned 404 for non-existent customer" -ForegroundColor Gray
    }
}

# Test 19: Error Handling - Invalid State Transition
Test-Endpoint "Test 19: Error Handling - Invalid State Transition" {
    # Try to verify an already approved customer
    $verifyRequest = @{
        verifiedBy = "admin@banking.nl"
    } | ConvertTo-Json

    try {
        $response = Invoke-RestMethod -Uri "$baseUrl/$($script:customerId)/verify" `
            -Method POST `
            -Body $verifyRequest `
            -ContentType "application/json"

        throw "Should have failed with invalid state"
    }
    catch {
        if ($_.Exception.Response.StatusCode -ne 400) {
            throw "Expected 400 Bad Request, got $($_.Exception.Response.StatusCode)"
        }
        Write-Host "  Correctly rejected invalid state transition (400)" -ForegroundColor Gray
    }
}

# Test 20: Error Handling - Expired Document
Test-Endpoint "Test 20: Error Handling - Expired Document" {
    $uploadRequest = @{
        customerId = $script:customerId
        documentType = "PASSPORT"
        documentNumber = "P99999999"
        issuingAuthority = "Old Government"
        issueDate = "2010-01-01"
        expiryDate = "2020-01-01"  # Expired
        documentUrl = "/documents/expired_passport.pdf"
    } | ConvertTo-Json

    try {
        $response = Invoke-RestMethod -Uri $docUrl `
            -Method POST `
            -Body $uploadRequest `
            -ContentType "application/json"

        throw "Should have failed with expired document"
    }
    catch {
        if ($_.Exception.Response.StatusCode -ne 400) {
            throw "Expected 400 Bad Request, got $($_.Exception.Response.StatusCode)"
        }
        Write-Host "  Correctly rejected expired document (400)" -ForegroundColor Gray
    }
}

# Summary
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "   Test Summary" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Total Tests: $($testsPassed + $testsFailed)" -ForegroundColor White
Write-Host "  ✓ Passed: $testsPassed" -ForegroundColor Green
Write-Host "  ✗ Failed: $testsFailed" -ForegroundColor Red
Write-Host "========================================`n" -ForegroundColor Cyan

if ($testsFailed -gt 0) {
    exit 1
}

Write-Host "All tests passed! ✓" -ForegroundColor Green
exit 0
