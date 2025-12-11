Write-Host "=== DIRECT ENDPOINT TEST ===" -ForegroundColor Cyan

# Account numarasını al
$accountNumber = "TR850000146625818780278023"

Write-Host "Calling: GET http://localhost:8081/api/v1/accounts/number/$accountNumber" -ForegroundColor Yellow

try {
    $response = Invoke-WebRequest -Uri "http://localhost:8081/api/v1/accounts/number/$accountNumber" `
        -Method GET `
        -ContentType "application/json" `
        -UseBasicParsing

    Write-Host "SUCCESS (Status: $($response.StatusCode))" -ForegroundColor Green
    Write-Host $response.Content | ConvertFrom-Json | ConvertTo-Json -Depth 5

} catch {
    Write-Host "FAILED (Status: $($_.Exception.Response.StatusCode.value__))" -ForegroundColor Red
    Write-Host "Error Message: $($_.Exception.Message)" -ForegroundColor Yellow

    # Response body
    $reader = [System.IO.StreamReader]::new($_.Exception.Response.GetResponseStream())
    $responseBody = $reader.ReadToEnd()
    Write-Host "Response Body: $responseBody" -ForegroundColor Cyan

    # Şimdi logları kontrol et
    Write-Host "`nChecking Account Service logs for the error..." -ForegroundColor Yellow
    Start-Sleep -Seconds 2
    docker logs banking-account-service --tail 50 | Select-String -Pattern "Unexpected error|Exception" -Context 2, 15
}