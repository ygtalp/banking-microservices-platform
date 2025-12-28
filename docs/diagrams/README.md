# Architecture Diagrams

This folder contains Mermaid diagram files for the Banking Microservices Platform.

## Available Diagrams

### 1. System Architecture (`system-architecture.mermaid`)
High-level system architecture showing:
- API Gateway
- Service Discovery (Eureka)
- Microservices (Account, Transfer)
- Data Layer (PostgreSQL, Redis)
- Messaging (Kafka)

**Use Case:** System overview, documentation, presentations

---

### 2. SAGA Flow (`saga-flow.mermaid`)
Sequence diagram showing:
- SAGA orchestration flow
- 3 steps: Validation, Debit, Credit
- Success path
- Compensation path (rollback)
- Idempotency check

**Use Case:** Technical interviews, SAGA pattern explanation

---

### 3. Event Flow (`event-flow.mermaid`)
Event-driven architecture showing:
- Event publishers (Account, Transfer)
- Kafka topics
- Event consumers (Notification, History, Analytics)

**Use Case:** Event architecture documentation

---

### 4. Deployment Architecture (`deployment-architecture.mermaid`)
Docker deployment showing:
- Application containers
- Data containers
- Messaging containers
- Monitoring containers
- External access

**Use Case:** DevOps documentation, deployment planning

---

## How to View Diagrams

### Option 1: GitHub (Automatic Rendering)
GitHub automatically renders `.mermaid` files:
```
https://github.com/{username}/{repo}/blob/main/docs/diagrams/system-architecture.mermaid
```

### Option 2: Mermaid Live Editor
1. Go to https://mermaid.live/
2. Copy diagram content
3. Paste and view

### Option 3: VS Code Extension
1. Install "Markdown Preview Mermaid Support" extension
2. Create markdown file with:
````markdown
```mermaid
[paste diagram content here]
```
````
3. Preview markdown (Ctrl+Shift+V)

### Option 4: IntelliJ IDEA
1. Install "Mermaid" plugin
2. Open `.mermaid` file
3. Right-click → "Open Mermaid Preview"

---

## Embedding in Documentation

### In Markdown Files

````markdown
# System Architecture

```mermaid
[paste diagram content]
```
````

### In GitHub README

````markdown
## Architecture

![System Architecture](docs/diagrams/system-architecture.mermaid)
````

### In Confluence

1. Install Mermaid macro
2. Add macro to page
3. Paste diagram content

---

## Exporting as Images

### Using Mermaid CLI

```bash
# Install
npm install -g @mermaid-js/mermaid-cli

# Export as PNG
mmdc -i system-architecture.mermaid -o system-architecture.png

# Export as SVG
mmdc -i system-architecture.mermaid -o system-architecture.svg

# Export all
for file in *.mermaid; do
  mmdc -i "$file" -o "${file%.mermaid}.png"
done
```

### Using Mermaid Live Editor

1. Open https://mermaid.live/
2. Paste diagram
3. Click "Actions" → "PNG" or "SVG"
4. Download image

---

## Updating Diagrams

When updating architecture:

1. **Update Diagram File**
   ```bash
   # Edit diagram
   vim docs/diagrams/system-architecture.mermaid
   ```

2. **Verify Rendering**
   - Open in Mermaid Live Editor
   - Check syntax
   - Verify colors and layout

3. **Commit Changes**
   ```bash
   git add docs/diagrams/system-architecture.mermaid
   git commit -m "docs: update system architecture diagram"
   git push
   ```

4. **Update Related Documentation**
   - Update ARCHITECTURE_DECISIONS.md if needed
   - Update README.md if needed
   - Update presentation slides

---

## Diagram Style Guide

### Colors Used

```
Green (#4CAF50):    API Gateway
Blue (#2196F3):     Service Discovery
Orange (#FF9800):   Microservices
Purple (#9C27B0):   Databases
Red (#F44336):      Redis
Cyan (#00BCD4):     Kafka
Yellow (#FFC107):   Monitoring
```

### Node Types

```
[Square]:           Services
[(Cylinder)]:       Databases
[Rounded]:          External systems
-->:                Synchronous calls
-.->:               Asynchronous calls
```

---

## Common Issues

### Issue: Diagram Not Rendering on GitHub
**Solution:** Ensure file has `.mermaid` extension and valid syntax

### Issue: Colors Not Showing
**Solution:** Add style definitions:
```mermaid
style NodeName fill:#color,stroke:#color,color:#fff
```

### Issue: Complex Diagram Timeout
**Solution:** Simplify diagram or split into multiple diagrams

---

## Resources

- [Mermaid Documentation](https://mermaid-js.github.io/)
- [Mermaid Live Editor](https://mermaid.live/)
- [Mermaid Cheat Sheet](https://jojozhuang.github.io/tutorial/mermaid-cheat-sheet/)
- [GitHub Mermaid Support](https://github.blog/2022-02-14-include-diagrams-markdown-files-mermaid/)

---

**Last Updated:** 23 December 2025  
**Diagram Count:** 4  
**Format:** Mermaid 9.x
