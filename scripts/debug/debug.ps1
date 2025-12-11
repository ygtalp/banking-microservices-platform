Write-Host "========================================" -ForegroundColor Cyan
Write-Host "BANKING PLATFORM - DIAGNOSTIC CHECK" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# 1. Transfer Details
Write-Host "`n[1/4] Transfer Details from Database:" -ForegroundColor Yellow
docker exec -it banking-postgres psql -U postgres -d banking_transfers -c "SELECT transfer_reference, from_account_number, to_account_number, amount, status, failure_reason, debit_transaction_id, credit_transaction_id, created_at FROM transfers ORDER BY created_at DESC LIMIT 5;"

# 2. Account Balances
Write-Host "`n[2/4] Account Balances:" -ForegroundColor Yellow
docker exec -it banking-postgres psql -U postgres -d banking_accounts -c "SELECT account_number, customer_name, balance, currency, status FROM accounts;"

# 3. Transfer Service Logs (last 30 lines)
Write-Host "`n[3/4] Transfer Service Logs (Recent):" -ForegroundColor Yellow
docker logs banking-transfer-service --tail 30

# 4. Account Service Logs (last 20 lines)
Write-Host "`n[4/4] Account Service Logs (Recent):" -ForegroundColor Yellow
docker logs banking-account-service --tail 20

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "DIAGNOSTIC CHECK COMPLETE" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan