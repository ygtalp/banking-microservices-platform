# Development Guide

> **Purpose:** Setup and daily development workflow  
> **Audience:** Developers  
> **Last Updated:** 23 December 2025

---

## Prerequisites

```yaml
Required:
  - Java 17 JDK
  - Maven 3.9+
  - Docker Desktop
  - Git
  - PowerShell 7+

Recommended:
  - IntelliJ IDEA Ultimate
  - Postman
  - DBeaver (database client)
```

---

## Quick Start

```powershell
# 1. Clone repository
git clone https://github.com/{username}/banking-microservices-platform.git
cd banking-microservices-platform

# 2. Start all services (one command!)
.\quick-start.ps1

# This does:
# - Maven build all services
# - Docker Compose up
# - Health checks
# - API tests
# - Status display
```

---

## Daily Workflow

### Build

```powershell
# Build all services
.\scripts\build\build-fixed.ps1

# Build specific service
cd account-service
mvn clean package -DskipTests
```

### Deploy

```powershell
# Deploy all services
.\scripts\deploy\deploy-fixed.ps1

# Restart specific service
docker-compose restart account-service
```

### Testing

```powershell
# Run API tests
.\scripts\test\test-services-fixed.ps1

# Unit tests
mvn test

# Integration tests
mvn verify
```

### Debugging

```powershell
# View logs
.\utils.ps1 -Command logs -Service account-service

# Database inspection
.\utils.ps1 -Command db

# Full diagnostic
.\scripts\debug\debug.ps1
```

---

## Git Workflow

### Branch Strategy

```bash
main        → Production-ready code
develop     → Integration branch
feature/*   → New features
fix/*       → Bug fixes
hotfix/*    → Critical production fixes
```

### Commit Convention

```bash
<type>(<scope>): <subject>

Types:
  feat:     New feature
  fix:      Bug fix
  docs:     Documentation
  style:    Formatting
  refactor: Code restructuring
  test:     Adding tests
  chore:    Maintenance

Examples:
  feat(account): add multi-currency support
  fix(transfer): resolve SAGA compensation issue
  docs(readme): update API documentation
```

---

## IDE Setup (IntelliJ)

### Plugins

```
Required:
  - Lombok
  - Spring Boot
  - Docker

Recommended:
  - SonarLint
  - Git Toolbox
  - Rainbow Brackets
```

### Run Configuration

```yaml
Name: Account Service
Main Class: AccountServiceApplication
VM Options: -Dspring.profiles.active=local
Working Directory: $MODULE_DIR$
```

---

## Troubleshooting

### Port Already in Use

```bash
# Windows
netstat -ano | findstr :8081
taskkill /PID <PID> /F

# Stop all Docker containers
docker-compose down
```

### Database Connection Issues

```bash
# Reset databases
docker-compose down -v
docker-compose up -d postgres
```

---

**Last Updated:** 23 December 2025  
**Status:** ✅ Complete
