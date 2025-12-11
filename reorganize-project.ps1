# Banking Microservices Platform - Project Reorganization
# Organizes scripts and logs into proper folder structure

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "PROJECT REORGANIZATION" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# Create folder structure
Write-Host ""
Write-Host "[1/4] Creating folder structure..." -ForegroundColor Yellow

$folders = @(
    "scripts",
    "scripts\build",
    "scripts\deploy",
    "scripts\test",
    "scripts\debug",
    "docs",
    "logs"
)

foreach ($folder in $folders) {
    if (-not (Test-Path $folder)) {
        New-Item -ItemType Directory -Path $folder -Force | Out-Null
        Write-Host "  Created: $folder" -ForegroundColor Green
    } else {
        Write-Host "  Exists: $folder" -ForegroundColor Gray
    }
}

# Move build scripts
Write-Host ""
Write-Host "[2/4] Organizing build scripts..." -ForegroundColor Yellow
$buildScripts = @(
    "build-fixed.ps1"
)

foreach ($script in $buildScripts) {
    if (Test-Path $script) {
        Move-Item $script "scripts\build\" -Force
        Write-Host "  Moved: $script -> scripts\build\" -ForegroundColor Green
    }
}

# Move deploy scripts
Write-Host ""
Write-Host "[3/4] Organizing deploy scripts..." -ForegroundColor Yellow
$deployScripts = @(
    "deploy-fixed.ps1",
    "fix-and-rebuild.ps1"
)

foreach ($script in $deployScripts) {
    if (Test-Path $script) {
        Move-Item $script "scripts\deploy\" -Force
        Write-Host "  Moved: $script -> scripts\deploy\" -ForegroundColor Green
    }
}

# Move test scripts
Write-Host ""
Write-Host "[4/4] Organizing test and debug scripts..." -ForegroundColor Yellow
$testScripts = @(
    "test-services-fixed.ps1"
)

foreach ($script in $testScripts) {
    if (Test-Path $script) {
        Move-Item $script "scripts\test\" -Force
        Write-Host "  Moved: $script -> scripts\test\" -ForegroundColor Green
    }
}

$debugScripts = @(
    "debug.ps1"
)

foreach ($script in $debugScripts) {
    if (Test-Path $script) {
        Move-Item $script "scripts\debug\" -Force
        Write-Host "  Moved: $script -> scripts\debug\" -ForegroundColor Green
    }
}

# Move log files
Write-Host ""
Write-Host "Organizing log files..." -ForegroundColor Yellow
$logFiles = @(
    "account-service-full.txt",
    "config-analysis-2025-12-09_181830.txt",
    "transfer-service-files-20251210_223055.txt"
)

foreach ($log in $logFiles) {
    if (Test-Path $log) {
        Move-Item $log "logs\" -Force
        Write-Host "  Moved: $log -> logs\" -ForegroundColor Green
    }
}

# Create scripts README
$scriptsReadme = @"
# Scripts Directory

## Structure

scripts/
  - build/          Build scripts
  - deploy/         Deployment scripts
  - test/           Test scripts
  - debug/          Debug scripts

## Usage

Build:    .\scripts\build\build-fixed.ps1
Deploy:   .\scripts\deploy\deploy-fixed.ps1
Test:     .\scripts\test\test-services-fixed.ps1
Debug:    .\scripts\debug\debug.ps1
"@

$scriptsReadme | Out-File -FilePath "scripts\README.md" -Encoding UTF8

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "REORGANIZATION COMPLETE!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green

Write-Host ""
Write-Host "New folder structure:" -ForegroundColor Cyan
Write-Host "  scripts/build/    - Build scripts" -ForegroundColor White
Write-Host "  scripts/deploy/   - Deployment scripts" -ForegroundColor White
Write-Host "  scripts/test/     - Test scripts" -ForegroundColor White
Write-Host "  scripts/debug/    - Debug scripts" -ForegroundColor White
Write-Host "  docs/             - Documentation" -ForegroundColor White
Write-Host "  logs/             - Log files" -ForegroundColor White

Write-Host ""
Write-Host "Usage examples:" -ForegroundColor Cyan
Write-Host "  .\scripts\build\build-fixed.ps1" -ForegroundColor White
Write-Host "  .\scripts\deploy\deploy-fixed.ps1" -ForegroundColor White
Write-Host "  .\scripts\test\test-services-fixed.ps1" -ForegroundColor White
Write-Host ""