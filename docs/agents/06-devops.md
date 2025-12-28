# DevOps Agents

> **Category:** Infrastructure & Deployment
> **Agent Count:** 4
> **Automation Level:** Medium-High (75%)
> **Last Updated:** 28 December 2025

---

## 1. DockerAgent 🐳

**Objective:** Generate production-ready Docker configurations.

**Outputs:**
1. **Dockerfile** (multi-stage build)
2. **docker-compose service** definition
3. **.dockerignore**
4. Build scripts

**Example Dockerfile:**
```dockerfile
# Stage 1: Builder
FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /app

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy JAR from builder
COPY --from=builder /app/target/*.jar app.jar

# Change ownership
RUN chown -R appuser:appgroup /app
USER appuser

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8085/actuator/health || exit 1

# JVM options (container-aware)
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

EXPOSE 8085

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

**docker-compose.yml addition:**
```yaml
notification-service:
  build:
    context: ./notification-service
    dockerfile: Dockerfile
  image: banking-platform-notification:latest
  container_name: notification-service
  ports:
    - "8085:8085"
  environment:
    SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/banking_notifications
    SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:29092
    SPRING_REDIS_HOST: redis
    EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka-server:8761/eureka/
  depends_on:
    - postgres
    - redis
    - kafka
    - eureka-server
  networks:
    - banking-network
  restart: unless-stopped
```

---

## 2. DatabaseAgent 🗄️

**Objective:** Generate database schemas, migrations, and seed data.

**Outputs:**
1. Liquibase changelogs
2. Rollback scripts
3. Index strategy
4. Seed data

**Example Migration:**
```xml
<changeSet id="001-create-notifications-schema" author="system">
    <createTable tableName="notifications">
        <column name="id" type="BIGSERIAL">
            <constraints primaryKey="true" nullable="false"/>
        </column>
        <column name="notification_id" type="VARCHAR(50)">
            <constraints unique="true" nullable="false"/>
        </column>
        <column name="user_id" type="VARCHAR(50)">
            <constraints nullable="false"/>
        </column>
        <column name="status" type="VARCHAR(20)">
            <constraints nullable="false"/>
        </column>
        <column name="created_at" type="TIMESTAMP" defaultValueComputed="CURRENT_TIMESTAMP">
            <constraints nullable="false"/>
        </column>
    </createTable>

    <createIndex tableName="notifications" indexName="idx_user_id">
        <column name="user_id"/>
    </createIndex>

    <createIndex tableName="notifications" indexName="idx_status">
        <column name="status"/>
    </createIndex>

    <rollback>
        <dropTable tableName="notifications"/>
    </rollback>
</changeSet>
```

**Seed Data:**
```xml
<changeSet id="002-insert-default-templates" author="system">
    <insert tableName="notification_templates">
        <column name="name" value="welcome-email"/>
        <column name="channel" value="EMAIL"/>
        <column name="subject_template" value="Welcome to Our Platform"/>
        <column name="body_template" value="Hello {{firstName}}, welcome!"/>
        <column name="active" valueBoolean="true"/>
    </insert>
</changeSet>
```

---

## 3. MonitoringAgent 📊

**Objective:** Setup observability (metrics, logging, tracing).

**Outputs:**
1. Actuator configuration
2. Prometheus metrics
3. Grafana dashboards
4. Alert rules

**Actuator Configuration:**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
  metrics:
    tags:
      application: ${spring.application.name}
    export:
      prometheus:
        enabled: true
```

**Custom Metrics:**
```java
@Component
public class NotificationMetrics {

    private final MeterRegistry meterRegistry;
    private final Counter notificationsSent;
    private final Counter notificationsFailed;

    public NotificationMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.notificationsSent = Counter.builder("notifications.sent")
            .description("Total notifications sent")
            .tag("channel", "all")
            .register(meterRegistry);

        this.notificationsFailed = Counter.builder("notifications.failed")
            .description("Total notifications failed")
            .register(meterRegistry);
    }

    public void recordSent(NotificationChannel channel) {
        notificationsSent.increment();
        meterRegistry.counter("notifications.sent", "channel", channel.name())
            .increment();
    }

    public void recordFailed(NotificationChannel channel) {
        notificationsFailed.increment();
        meterRegistry.counter("notifications.failed", "channel", channel.name())
            .increment();
    }
}
```

**Grafana Dashboard JSON:**
```json
{
  "dashboard": {
    "title": "Notification Service Metrics",
    "panels": [
      {
        "title": "Notifications Sent (Rate)",
        "targets": [
          {
            "expr": "rate(notifications_sent_total[5m])",
            "legendFormat": "{{channel}}"
          }
        ]
      },
      {
        "title": "Failure Rate",
        "targets": [
          {
            "expr": "rate(notifications_failed_total[5m]) / rate(notifications_sent_total[5m])",
            "legendFormat": "Failure %"
          }
        ]
      }
    ]
  }
}
```

---

## 4. DeploymentAgent 🚀

**Objective:** Generate deployment scripts and strategies.

**Outputs:**
1. Build scripts (PowerShell)
2. Deploy scripts
3. Health check scripts
4. Rollback procedures

**Build Script (PowerShell):**
```powershell
# build-notification-service.ps1

Write-Host "Building Notification Service..." -ForegroundColor Cyan

# Step 1: Maven build
Write-Host "`n[1/3] Maven build..." -ForegroundColor Yellow
cd notification-service
mvn clean package -DskipTests

if ($LASTEXITCODE -ne 0) {
    Write-Host "  ❌ Maven build failed" -ForegroundColor Red
    exit 1
}
Write-Host "  ✅ Maven build successful" -ForegroundColor Green

# Step 2: Docker build
Write-Host "`n[2/3] Docker build..." -ForegroundColor Yellow
cd ..
docker-compose build notification-service

if ($LASTEXITCODE -ne 0) {
    Write-Host "  ❌ Docker build failed" -ForegroundColor Red
    exit 1
}
Write-Host "  ✅ Docker build successful" -ForegroundColor Green

# Step 3: Verify image
Write-Host "`n[3/3] Verifying image..." -ForegroundColor Yellow
$image = docker images -q banking-platform-notification:latest

if ($image) {
    Write-Host "  ✅ Image created successfully" -ForegroundColor Green
    docker images banking-platform-notification:latest
} else {
    Write-Host "  ❌ Image not found" -ForegroundColor Red
    exit 1
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "BUILD COMPLETED SUCCESSFULLY" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
```

**Deploy Script:**
```powershell
# deploy-notification-service.ps1

Write-Host "Deploying Notification Service..." -ForegroundColor Cyan

# Step 1: Stop existing container
Write-Host "`n[1/4] Stopping existing container..." -ForegroundColor Yellow
docker-compose stop notification-service
docker-compose rm -f notification-service

# Step 2: Start new container
Write-Host "`n[2/4] Starting new container..." -ForegroundColor Yellow
docker-compose up -d notification-service

# Step 3: Wait for startup
Write-Host "`n[3/4] Waiting for service startup..." -ForegroundColor Yellow
$maxRetries = 30
$retries = 0

while ($retries -lt $maxRetries) {
    try {
        $response = Invoke-RestMethod -Uri "http://localhost:8085/actuator/health" `
            -Method GET -TimeoutSec 2 -ErrorAction Stop

        if ($response.status -eq "UP") {
            Write-Host "  ✅ Service is UP" -ForegroundColor Green
            break
        }
    } catch {
        $retries++
        Write-Host "  Retry $retries/$maxRetries..." -ForegroundColor Yellow
        Start-Sleep -Seconds 2
    }
}

if ($retries -eq $maxRetries) {
    Write-Host "  ❌ Service failed to start" -ForegroundColor Red
    docker logs notification-service --tail 50
    exit 1
}

# Step 4: Verify Eureka registration
Write-Host "`n[4/4] Verifying Eureka registration..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

$eureka = Invoke-RestMethod -Uri "http://localhost:8761/eureka/apps/NOTIFICATION-SERVICE" `
    -Headers @{Accept = "application/json"}

if ($eureka) {
    Write-Host "  ✅ Registered with Eureka" -ForegroundColor Green
} else {
    Write-Host "  ⚠️  Not registered with Eureka yet" -ForegroundColor Yellow
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "DEPLOYMENT COMPLETED" -ForegroundColor Green
Write-Host "Service URL: http://localhost:8085" -ForegroundColor White
Write-Host "Health: http://localhost:8085/actuator/health" -ForegroundColor White
Write-Host "========================================" -ForegroundColor Cyan
```

**Rollback Procedure:**
```powershell
# rollback-notification-service.ps1

param(
    [Parameter(Mandatory=$true)]
    [string]$Version
)

Write-Host "Rolling back to version: $Version" -ForegroundColor Yellow

# Pull previous version
docker pull banking-platform-notification:$Version

# Update docker-compose to use specific version
(Get-Content docker-compose.yml) -replace `
    'banking-platform-notification:latest', `
    "banking-platform-notification:$Version" | `
    Set-Content docker-compose.yml

# Redeploy
.\scripts\deploy\deploy-notification-service.ps1
```

---

**Next:** [Debugging Agents →](./07-debugging.md)
