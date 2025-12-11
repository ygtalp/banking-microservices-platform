Write-Host "=== SEARCHING FULL ACCOUNT SERVICE LOG ===" -ForegroundColor Cyan

# Tüm log'u al ve Exception'ları ara
docker logs banking-account-service 2>&1 | Select-String -Pattern "Unexpected error|NullPointer|Cannot|Error" -Context 3

# Eğer çok uzunsa, dosyaya kaydet
Write-Host "`nSaving full log to file..." -ForegroundColor Yellow
docker logs banking-account-service > account-service-full.txt 2>&1
Write-Host "Saved to: account-service-full.txt" -ForegroundColor Green

# Dosyada "Unexpected error" ara
Write-Host "`nSearching for exceptions in log file..." -ForegroundColor Yellow
Select-String -Path "account-service-full.txt" -Pattern "Unexpected|Exception|at com.banking" | Select-Object -First 50