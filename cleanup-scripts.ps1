# Banking Microservices Platform - Cleanup Obsolete Scripts
# Removes old/obsolete scripts and keeps only working versions

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "SCRIPT CLEANUP" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# Scripts to remove
$scriptsToRemove = @(
    "build.ps1",
    "deploy.ps1",
    "test-services.ps1",
    "debug2.ps1",
    "alternatif-hizli-test.ps1"
)

Write-Host ""
Write-Host "Scripts to be removed:" -ForegroundColor Yellow
foreach ($script in $scriptsToRemove) {
    if (Test-Path $script) {
        Write-Host "  [X] $script" -ForegroundColor Red
    } else {
        Write-Host "  [ ] $script (not found)" -ForegroundColor Gray
    }
}

Write-Host ""
Write-Host "Scripts to be kept:" -ForegroundColor Yellow
$scriptsToKeep = @(
    "build-fixed.ps1",
    "deploy-fixed.ps1",
    "fix-and-rebuild.ps1",
    "test-services-fixed.ps1",
    "debug.ps1",
    "quick-start.ps1",
    "utils.ps1",
    "reorganize-project.ps1"
)

foreach ($script in $scriptsToKeep) {
    if (Test-Path $script) {
        Write-Host "  [OK] $script" -ForegroundColor Green
    } else {
        Write-Host "  [??] $script (not found)" -ForegroundColor Yellow
    }
}

Write-Host ""
$confirm = Read-Host "Continue with cleanup? (Y/n)"

if ($confirm -eq "n" -or $confirm -eq "N") {
    Write-Host "Cleanup cancelled." -ForegroundColor Yellow
    exit 0
}

# Perform cleanup
Write-Host ""
Write-Host "Removing obsolete scripts..." -ForegroundColor Yellow
$removedCount = 0

foreach ($script in $scriptsToRemove) {
    if (Test-Path $script) {
        Remove-Item $script -Force
        Write-Host "  Removed: $script" -ForegroundColor Red
        $removedCount++
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "CLEANUP COMPLETE!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Removed $removedCount script(s)" -ForegroundColor Cyan

Write-Host ""
Write-Host "Remaining scripts:" -ForegroundColor Cyan
Write-Host "  Root: quick-start.ps1, utils.ps1, reorganize-project.ps1" -ForegroundColor White
Write-Host "  scripts/build/: build-fixed.ps1" -ForegroundColor White
Write-Host "  scripts/deploy/: deploy-fixed.ps1, fix-and-rebuild.ps1" -ForegroundColor White
Write-Host "  scripts/test/: test-services-fixed.ps1" -ForegroundColor White
Write-Host "  scripts/debug/: debug.ps1" -ForegroundColor White

Write-Host ""
Write-Host "Next step: Run '.\reorganize-project.ps1' to organize remaining scripts" -ForegroundColor Yellow
Write-Host ""