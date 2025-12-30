# Observability & Monitoring - Banking Microservices Platform

**Status:** ✅ PRODUCTION-READY
**Last Updated:** 30 December 2025
**Version:** 1.0.0

---

## 📋 TABLE OF CONTENTS

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Components](#components)
4. [Distributed Tracing](#distributed-tracing)
5. [Centralized Logging](#centralized-logging)
6. [Metrics & Dashboards](#metrics--dashboards)
7. [Configuration](#configuration)
8. [Deployment](#deployment)
9. [Access & Usage](#access--usage)
10. [Best Practices](#best-practices)
11. [Troubleshooting](#troubleshooting)

---

## 🎯 OVERVIEW

Complete observability stack for the Banking Microservices Platform, providing:

- **Distributed Tracing** with Zipkin (trace requests across services)
- **Centralized Logging** with ELK Stack (aggregate logs from all services)
- **Metrics Collection** with Prometheus (time-series metrics)
- **Visualization** with Grafana (dashboards and alerts)

### Why Observability?

In a microservices architecture with 5+ services, observability is critical for:
- **Debugging:** Trace requests across service boundaries
- **Performance:** Identify bottlenecks and slow operations
- **Reliability:** Monitor service health and detect failures
- **Business Insights:** Track transfer rates, authentication patterns, etc.

---

## 🏗 ARCHITECTURE

```
┌──────────────────────────────────────────────────────────┐
│                  Banking Services                         │
│  (Account, Transfer, Customer, Auth, Notification)       │
└───┬──────────────┬──────────────┬──────────────┬─────────┘
    │              │              │              │
    │ Traces       │ Logs         │ Metrics      │
    ▼              ▼              ▼              │
┌─────────┐   ┌──────────┐   ┌──────────┐       │
│ Zipkin  │   │ Logstash │   │Prometheus│       │
│ (9411)  │   │ (5000)   │   │ (9090)   │       │
└─────────┘   └────┬─────┘   └────┬─────┘       │
                   │              │              │
                   ▼              │              │
              ┌────────────┐      │              │
              │Elasticsearch│     │              │
              │   (9200)   │      │              │
              └─────┬──────┘      │              │
                    │             │              │
                    ▼             ▼              ▼
              ┌───────────────────────────────────┐
              │         Grafana (3000)            │
              │  Unified Visualization Layer      │
              └───────────────────────────────────┘
                           │
                           ▼
              ┌───────────────────────┐
              │  Kibana (5601)        │
              │  Log Analysis UI      │
              └───────────────────────┘
```

### Data Flow

1. **Services** → Generate traces, logs, and metrics
2. **Zipkin** → Collects distributed traces
3. **Logstash** → Receives logs → **Elasticsearch** (storage)
4. **Prometheus** → Scrapes metrics from services
5. **Grafana** → Visualizes everything (Prometheus + Elasticsearch)
6. **Kibana** → Alternative log visualization

---

## 📦 COMPONENTS

### 1. Zipkin (Distributed Tracing)

**Purpose:** Track request flow across microservices

**Port:** 9411
**UI:** http://localhost:9411

**Features:**
- Automatic trace ID generation
- Span correlation across services
- Service dependency visualization
- Latency analysis
- Error tracking

**Implementation:**
- **Library:** Micrometer Tracing Bridge (Brave)
- **Reporter:** Zipkin Reporter Brave
- **Sampling:** 100% (development), configurable for production
- **Storage:** In-memory (can be configured for persistent storage)

### 2. ELK Stack (Centralized Logging)

#### Elasticsearch

**Purpose:** Log storage and search engine

**Ports:** 9200 (HTTP), 9300 (Transport)
**Version:** 8.11.0

**Configuration:**
- Single-node cluster (development)
- Security disabled (enable in production)
- Heap: 512MB (JVM)
- Index pattern: `banking-logs-YYYY.MM.dd`

#### Logstash

**Purpose:** Log processing and aggregation

**Ports:** 5000 (TCP input), 9600 (Monitoring)
**Version:** 8.11.0

**Pipeline:**
```
Input (TCP 5000) → Filter (JSON parsing, trace extraction) → Output (Elasticsearch)
```

**Features:**
- JSON log parsing
- Trace ID extraction (correlate with Zipkin)
- Service name extraction
- Log level normalization

#### Kibana

**Purpose:** Log visualization and analysis

**Port:** 5601
**UI:** http://localhost:5601
**Version:** 8.11.0

**Features:**
- Log search and filtering
- Index pattern management
- Visualizations and dashboards
- Time-series analysis

### 3. Prometheus (Metrics Collection)

**Purpose:** Time-series metrics database

**Port:** 9090
**UI:** http://localhost:9090
**Version:** Latest

**Scrape Targets:**
- All 5 microservices (every 15 seconds)
- API Gateway
- Eureka Server

**Metrics Collected:**
- JVM metrics (memory, GC, threads, CPU)
- HTTP metrics (request rate, latency, errors)
- Custom business metrics
- Spring Boot Actuator metrics

**Configuration:** `observability/prometheus/prometheus.yml`

### 4. Grafana (Visualization)

**Purpose:** Unified dashboard and alerting

**Port:** 3000
**UI:** http://localhost:3000
**Credentials:** admin / admin
**Version:** Latest

**Data Sources:**
- Prometheus (metrics)
- Elasticsearch (logs)

**Pre-built Dashboards:**
1. **System Overview** - Service status, request rates, latency
2. **JVM Metrics** - Heap/non-heap memory, GC, threads, CPU
3. **Business Metrics** - Transfers, accounts, auth operations

**Location:** `observability/grafana/dashboards/`

---

## 🔍 DISTRIBUTED TRACING

### How It Works

Every request gets a unique **Trace ID** that flows through all services:

```
Client Request → API Gateway → Account Service → Transfer Service
                 [Trace: abc123]  [Trace: abc123]  [Trace: abc123]
                 [Span: 1]        [Span: 2]        [Span: 3]
```

### Dependencies Added

All 5 services have these dependencies in `pom.xml`:

```xml
<!-- Distributed Tracing -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>
<dependency>
    <groupId>io.zipkin.reporter2</groupId>
    <artifactId>zipkin-reporter-brave</artifactId>
</dependency>
```

### Configuration

Each service's `application.yml`:

```yaml
management:
  tracing:
    sampling:
      probability: 1.0  # 100% sampling
  zipkin:
    tracing:
      endpoint: ${ZIPKIN_URL:http://localhost:9411/api/v2/spans}
```

### Using Zipkin UI

**1. Access:** http://localhost:9411

**2. Search for traces:**
- By service name (e.g., `account-service`)
- By time range
- By latency (find slow requests)
- By trace ID

**3. Analyze a trace:**
- See all spans (service calls)
- Identify bottlenecks
- View timing breakdown
- Check errors

**Example Trace:**
```
Transfer Creation (450ms total)
  ├─ API Gateway (50ms)
  ├─ Transfer Service (350ms)
  │   ├─ Validate Account (100ms) → Account Service
  │   ├─ Debit Account (100ms) → Account Service
  │   └─ Credit Account (100ms) → Account Service
  └─ Notification (50ms) → Notification Service
```

---

## 📝 CENTRALIZED LOGGING

### Log Structure

Services send structured JSON logs:

```json
{
  "timestamp": "2025-12-30T14:30:45.123Z",
  "level": "INFO",
  "service_name": "account-service",
  "logger_name": "com.banking.account.service.AccountService",
  "message": "Account created successfully",
  "traceId": "abc123",
  "spanId": "xyz789",
  "accountNumber": "ACC-1234567890"
}
```

### Logstash Pipeline

**Location:** `observability/logstash/pipeline/logstash.conf`

**Processing Steps:**
1. Receive logs via TCP (port 5000)
2. Parse JSON structure
3. Extract trace/span IDs
4. Normalize log levels
5. Index in Elasticsearch

### Kibana Usage

**1. Access:** http://localhost:5601

**2. Create Index Pattern:**
- Navigate to **Management** → **Index Patterns**
- Pattern: `banking-logs-*`
- Time field: `@timestamp`

**3. Discover Logs:**
- Go to **Discover**
- Filter by service: `service_name: "account-service"`
- Filter by trace: `traceId: "abc123"`
- Filter by level: `level: "ERROR"`

**4. Create Visualizations:**
- Error rate over time
- Logs by service
- Top error messages

---

## 📊 METRICS & DASHBOARDS

### Prometheus Metrics

**Endpoint:** Each service exposes `/actuator/prometheus`

**Example Metrics:**

```
# JVM Memory
jvm_memory_used_bytes{area="heap",service="account-service"}
jvm_memory_max_bytes{area="heap",service="account-service"}

# HTTP Requests
http_server_requests_seconds_count{uri="/accounts",method="POST"}
http_server_requests_seconds_sum{uri="/accounts",method="POST"}

# Custom Business Metrics (future)
transfer_total{status="SUCCESS"}
transfer_total{status="FAILED"}
```

### Dependencies Added

```xml
<!-- Metrics -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

### Grafana Dashboards

**Location:** `observability/grafana/dashboards/`

#### 1. System Overview (`system-overview.json`)

**Panels:**
- Services Status (up/down)
- HTTP Request Rate (req/s)
- HTTP Response Time (p95)
- JVM Memory Usage
- Error Rate (%)

**Use Case:** Quick health check of entire platform

#### 2. JVM Metrics (`jvm-metrics.json`)

**Panels:**
- Heap Memory Usage (used vs max)
- Non-Heap Memory Usage
- GC Count (garbage collection frequency)
- GC Duration (pause times)
- Thread Count (live vs daemon)
- CPU Usage

**Use Case:** Debug memory leaks, GC issues, performance

#### 3. Business Metrics (`business-metrics.json`)

**Panels:**
- Account Operations (rate/min)
- Transfer Operations (rate/min)
- Transfer Success Rate (%)
- Authentication Operations (rate/min)
- Notification Operations (rate/min)
- Customer KYC Operations (rate/min)

**Use Case:** Business analytics, trend analysis

### Creating Custom Dashboards

**1. Access Grafana:** http://localhost:3000

**2. Create Dashboard:**
- Click **+** → **Dashboard**
- Add panel → Select **Prometheus** data source

**3. Example Query (Request Rate):**
```promql
rate(http_server_requests_seconds_count{job="account-service"}[5m])
```

**4. Example Query (Memory Usage %):**
```promql
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} * 100
```

**5. Save Dashboard** to `observability/grafana/dashboards/`

---

## ⚙️ CONFIGURATION

### Docker Compose

**Observability Services Added:**

```yaml
services:
  zipkin:
    image: openzipkin/zipkin:latest
    ports: ["9411:9411"]

  prometheus:
    image: prom/prometheus:latest
    ports: ["9090:9090"]
    volumes:
      - ./observability/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml

  grafana:
    image: grafana/grafana:latest
    ports: ["3000:3000"]
    environment:
      GF_SECURITY_ADMIN_PASSWORD: admin

  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.11.0
    ports: ["9200:9200", "9300:9300"]

  logstash:
    image: docker.elastic.co/logstash/logstash:8.11.0
    ports: ["5000:5000", "9600:9600"]

  kibana:
    image: docker.elastic.co/kibana/kibana:8.11.0
    ports: ["5601:5601"]
```

### Environment Variables

All services have these environment variables set in `docker-compose.yml`:

```yaml
ZIPKIN_URL: http://zipkin:9411/api/v2/spans
```

### Prometheus Configuration

**File:** `observability/prometheus/prometheus.yml`

**Scrape Configuration:**
```yaml
scrape_configs:
  - job_name: 'account-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['account-service:8081']
  # ... (same for all services)
```

### Grafana Provisioning

**Data Sources:** `observability/grafana/provisioning/datasources/datasource.yml`

```yaml
datasources:
  - name: Prometheus
    type: prometheus
    url: http://prometheus:9090
    isDefault: true
  - name: Elasticsearch
    type: elasticsearch
    url: http://elasticsearch:9200
    database: "banking-logs-*"
```

**Dashboards:** `observability/grafana/provisioning/dashboards/dashboard.yml`

Auto-loads all dashboards from `observability/grafana/dashboards/`

---

## 🚀 DEPLOYMENT

### Full Stack Deployment

```powershell
# Build all services
mvn clean package -DskipTests

# Start observability stack + services
docker-compose up -d

# Check container status
docker ps
```

### Services Start Order

```
1. Infrastructure: postgres, redis, zookeeper, kafka
2. Observability: zipkin, prometheus, elasticsearch, logstash
3. Discovery: eureka-server
4. Gateway: api-gateway
5. Services: account, transfer, customer, auth, notification
6. Visualization: grafana, kibana
```

### Health Checks

```powershell
# Zipkin
curl http://localhost:9411/health

# Prometheus
curl http://localhost:9090/-/healthy

# Elasticsearch
curl http://localhost:9200/_cluster/health

# Kibana
curl http://localhost:5601/api/status

# Grafana
curl http://localhost:3000/api/health

# Services
curl http://localhost:8081/actuator/health  # Account
curl http://localhost:8082/actuator/health  # Transfer
curl http://localhost:8083/actuator/health  # Customer
curl http://localhost:8084/actuator/health  # Auth
curl http://localhost:8085/actuator/health  # Notification
```

### Verify Tracing

```powershell
# 1. Create an account (generates trace)
curl -X POST http://localhost:8081/accounts -H "Content-Type: application/json" -d '{...}'

# 2. Check Zipkin for trace
# Open http://localhost:9411
# Search for "account-service"
# Click on trace to see spans
```

### Verify Metrics

```powershell
# 1. Check Prometheus targets
# Open http://localhost:9090/targets
# All services should be "UP"

# 2. Query metrics
# Open http://localhost:9090/graph
# Enter: jvm_memory_used_bytes{area="heap"}
# Click "Execute"

# 3. View Grafana dashboards
# Open http://localhost:3000
# Login: admin/admin
# Browse dashboards
```

### Verify Logging

```powershell
# 1. Create Kibana index pattern
# Open http://localhost:5601
# Management → Index Patterns → Create
# Pattern: banking-logs-*
# Time field: @timestamp

# 2. View logs
# Discover → Select time range
# Add filter: service_name is "account-service"
```

---

## 🖥 ACCESS & USAGE

### Observability Endpoints

| Component | URL | Credentials | Purpose |
|-----------|-----|-------------|---------|
| **Zipkin** | http://localhost:9411 | None | Distributed tracing |
| **Prometheus** | http://localhost:9090 | None | Metrics database |
| **Grafana** | http://localhost:3000 | admin/admin | Dashboards |
| **Elasticsearch** | http://localhost:9200 | None | Log storage |
| **Kibana** | http://localhost:5601 | None | Log analysis |
| **Logstash** | http://localhost:9600 | None | Pipeline monitoring |

### Service Actuator Endpoints

All services expose:
- `/actuator/health` - Health status
- `/actuator/info` - Service information
- `/actuator/metrics` - Metrics list
- `/actuator/prometheus` - Prometheus format metrics

### Common Workflows

#### 1. Debug Slow Request

**Step 1:** Find slow traces in Zipkin
- Open http://localhost:9411
- Search by minimum duration (e.g., >500ms)
- Click on slow trace

**Step 2:** Identify bottleneck
- Review span durations
- Find longest span (e.g., database query)

**Step 3:** Check logs
- Copy trace ID from Zipkin
- Search in Kibana: `traceId: "abc123"`
- Review detailed logs

**Step 4:** Investigate metrics
- Open Grafana → JVM Metrics
- Check if memory/GC is causing slowness

#### 2. Troubleshoot Service Error

**Step 1:** Check service health
```bash
curl http://localhost:8081/actuator/health
```

**Step 2:** Search error logs in Kibana
- Filter: `level: "ERROR" AND service_name: "account-service"`
- Review stack traces

**Step 3:** Find related traces in Zipkin
- Search by service name
- Filter by error status

**Step 4:** Check metrics in Grafana
- System Overview → Error Rate panel
- Correlate with request rate spikes

#### 3. Monitor Business KPIs

**Grafana → Business Metrics Dashboard:**
- Transfer success rate trending
- Peak transaction hours
- Service usage patterns

**Kibana → Create Visualization:**
- Count of transfers by status
- Top error messages
- User activity patterns

---

## 🎯 BEST PRACTICES

### 1. Tracing

**DO:**
- Keep 100% sampling in development
- Use 1-10% sampling in production (high traffic)
- Add custom spans for important operations
- Include correlation IDs in logs

**DON'T:**
- Sample too aggressively (miss important traces)
- Store traces forever (expensive)
- Add too many custom tags (overhead)

### 2. Logging

**DO:**
- Use structured JSON logging
- Include trace/span IDs in logs
- Log at appropriate levels (ERROR, WARN, INFO, DEBUG)
- Sanitize sensitive data (passwords, tokens)

**DON'T:**
- Log full request/response bodies (PII)
- Log excessively (disk/network overhead)
- Use inconsistent log formats

**Log Levels:**
- `ERROR`: Failures requiring immediate attention
- `WARN`: Potential issues, degraded functionality
- `INFO`: Important state changes, business events
- `DEBUG`: Detailed diagnostic information

### 3. Metrics

**DO:**
- Expose business metrics (transfer count, success rate)
- Monitor key JVM metrics (heap, GC)
- Set up alerts for critical thresholds
- Use meaningful metric names

**DON'T:**
- Create too many metrics (cardinality explosion)
- Use high-cardinality labels (user IDs, account numbers)
- Forget to document custom metrics

**Key Metrics to Monitor:**
- Request rate (req/s)
- Response time (p50, p95, p99)
- Error rate (%)
- JVM heap usage (%)
- GC pause duration (ms)

### 4. Dashboards

**DO:**
- Create role-specific dashboards (dev, ops, business)
- Use consistent color schemes
- Add annotations for deployments
- Set up alerts on dashboards

**DON'T:**
- Overcrowd dashboards (too many panels)
- Use vanity metrics (not actionable)
- Forget to version control dashboards (JSON files)

---

## 🐛 TROUBLESHOOTING

### Zipkin Not Receiving Traces

**Symptom:** No traces in Zipkin UI

**Checks:**
1. Zipkin container running:
   ```bash
   docker ps | grep zipkin
   ```

2. Services can reach Zipkin:
   ```bash
   docker exec account-service curl http://zipkin:9411/health
   ```

3. Environment variable set:
   ```bash
   docker exec account-service printenv | grep ZIPKIN
   ```

**Fix:**
- Restart services with correct `ZIPKIN_URL`
- Check docker network connectivity

### Prometheus Not Scraping

**Symptom:** Targets showing as DOWN in Prometheus

**Checks:**
1. Target endpoints accessible:
   ```bash
   curl http://localhost:8081/actuator/prometheus
   ```

2. Prometheus configuration correct:
   ```bash
   docker exec prometheus cat /etc/prometheus/prometheus.yml
   ```

**Fix:**
- Verify service names in `prometheus.yml`
- Ensure services expose `/actuator/prometheus`
- Check Spring Security doesn't block actuator endpoints

### Elasticsearch Issues

**Symptom:** Kibana can't connect to Elasticsearch

**Checks:**
1. Elasticsearch health:
   ```bash
   curl http://localhost:9200/_cluster/health
   ```

2. Kibana logs:
   ```bash
   docker logs kibana
   ```

**Fix:**
- Increase Elasticsearch heap if OOM
- Check `discovery.type=single-node` is set
- Verify security is disabled for development

### Grafana Datasource Error

**Symptom:** "Bad Gateway" when querying Prometheus/Elasticsearch

**Checks:**
1. Datasource URLs correct in Grafana
2. Backend services accessible from Grafana container

**Fix:**
- Use container names, not `localhost`
- Example: `http://prometheus:9090` (not `http://localhost:9090`)

### High Memory Usage

**Symptom:** Observability stack consuming too much memory

**Solutions:**
- **Elasticsearch:** Reduce heap size in docker-compose:
  ```yaml
  environment:
    ES_JAVA_OPTS: "-Xms256m -Xmx256m"
  ```

- **Logstash:** Reduce heap:
  ```yaml
  environment:
    LS_JAVA_OPTS: "-Xms128m -Xms128m"
  ```

- **Prometheus:** Reduce retention:
  ```yaml
  command:
    - '--storage.tsdb.retention.time=7d'  # instead of default 15d
  ```

---

## 📈 FUTURE ENHANCEMENTS

### 1. Alerting

**Prometheus Alertmanager:**
- Email/Slack notifications
- Alert rules for critical metrics
- Alert routing by severity

**Example Alert:**
```yaml
- alert: HighErrorRate
  expr: rate(http_server_requests_seconds_count{status="5xx"}[5m]) > 0.05
  for: 5m
  annotations:
    summary: "High error rate on {{ $labels.service }}"
```

### 2. Tracing Enhancements

- **Persistent Storage:** Switch Zipkin to Elasticsearch backend
- **Custom Spans:** Add spans for database queries, external API calls
- **Baggage:** Propagate custom context (user ID, tenant ID)

### 3. Logging Enhancements

- **Structured Fields:** Add more metadata (request ID, user agent)
- **Log Aggregation:** Send logs from multiple environments
- **Log Sampling:** Sample DEBUG logs in production

### 4. Advanced Dashboards

- **SLA Dashboard:** Track service level objectives (99.9% uptime)
- **Capacity Planning:** Predict resource needs
- **Cost Analysis:** Track infrastructure costs

### 5. APM Integration

- **Elastic APM:** Application Performance Monitoring
- **OpenTelemetry:** Vendor-neutral observability
- **Jaeger:** Alternative to Zipkin with more features

---

## 📚 RESOURCES

### Official Documentation

- **Zipkin:** https://zipkin.io/
- **Prometheus:** https://prometheus.io/docs/
- **Grafana:** https://grafana.com/docs/
- **Elasticsearch:** https://www.elastic.co/guide/en/elasticsearch/reference/current/index.html
- **Logstash:** https://www.elastic.co/guide/en/logstash/current/index.html
- **Kibana:** https://www.elastic.co/guide/en/kibana/current/index.html
- **Micrometer:** https://micrometer.io/docs

### Useful Links

- **PromQL Tutorial:** https://prometheus.io/docs/prometheus/latest/querying/basics/
- **Grafana Dashboard Examples:** https://grafana.com/grafana/dashboards/
- **Zipkin Architecture:** https://zipkin.io/pages/architecture.html
- **ELK Best Practices:** https://www.elastic.co/guide/en/elasticsearch/guide/current/index.html

---

## 🎉 SUMMARY

**Observability Stack Provides:**
- ✅ **360° Visibility** into microservices
- ✅ **Fast Debugging** with distributed tracing
- ✅ **Centralized Logs** for all services
- ✅ **Real-time Metrics** and alerting
- ✅ **Beautiful Dashboards** for all stakeholders

**Quick Access:**
- Zipkin: http://localhost:9411
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000 (admin/admin)
- Kibana: http://localhost:5601
- Elasticsearch: http://localhost:9200

**Total Containers:** 18 (5 services + 7 infrastructure + 6 observability)

**Production Ready:** Yes, with proper configuration adjustments

---

**Last Updated:** 30 December 2025
**Maintained By:** Banking Platform Team
**Version:** 1.0.0
