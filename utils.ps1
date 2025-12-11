# Banking Microservices Platform - Utility Commands
# Quick access to common operations

param(
    [Parameter(Mandatory=$true)]
    [ValidateSet("logs", "restart", "status", "cleanup", "db")]
    [string]$Command,

    [Parameter(Mandatory=$false)]
    [string]$Service = "all"
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "BANKING MICROSERVICES - UTILS" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

switch ($Command) {
    "logs" {
        Write-Host ""
        Write-Host "Viewing logs for: $Service" -ForegroundColor Yellow

        if ($Service -eq "all") {
            docker-compose logs -f
        } else {
            docker-compose logs -f "banking-$Service"
        }
    }

    "restart" {
        Write-Host ""
        Write-Host "Restarting: $Service" -ForegroundColor Yellow

        if ($Service -eq "all") {
            docker-compose restart
            Write-Host "All services restarted" -ForegroundColor Green
        } else {
            docker restart "banking-$Service"
            Write-Host "Service restarted: $Service" -ForegroundColor Green
        }
    }

    "status" {
        Write-Host ""
        Write-Host "Service Status:" -ForegroundColor Yellow
        docker-compose ps
    }

    "cleanup" {
        Write-Host ""
        Write-Host "WARNING: This will remove all containers and volumes!" -ForegroundColor Red
        $confirm = Read-Host "Continue? (yes/no)"

        if ($confirm -eq "yes") {
            docker-compose down -v
            Write-Host "Cleanup complete" -ForegroundColor Green
        } else {
            Write-Host "Cleanup cancelled" -ForegroundColor Yellow
        }
    }

    "db" {
        Write-Host ""
        Write-Host "Connecting to PostgreSQL..." -ForegroundColor Yellow
        Write-Host "Use 'postgres' as password" -ForegroundColor Gray
        docker exec -it banking-postgres psql -U postgres
    }
}

Write-Host ""