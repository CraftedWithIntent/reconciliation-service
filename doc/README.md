# Documentation Guide

This folder contains the essential documentation for the Reconciliation Service. All files here should be committed to the repository.

## 📖 Quick Navigation

### Getting Started
- [**00_START_HERE.txt**](00_START_HERE.txt) - Project overview and entry point
- [**QUICKSTART.md**](QUICKSTART.md) - Fast setup guide (5 minutes)
- [**QUICK_REFERENCE.md**](QUICK_REFERENCE.md) - Short command reference

### For Developers
- [**ARCHITECTURE_GUIDE.md**](ARCHITECTURE_GUIDE.md) - System design and components
- [**IMPLEMENTATION_NOTES.md**](IMPLEMENTATION_NOTES.md) - Implementation details
- [**FIELD_CONFIGURATION_GUIDE.md**](FIELD_CONFIGURATION_GUIDE.md) - Configuring reconciliation fields
- [**DOMAIN_MODEL_USAGE.md**](DOMAIN_MODEL_USAGE.md) - Understanding the domain model

### For Operations
- [**BUILD_AND_DEPLOY.md**](BUILD_AND_DEPLOY.md) - Building and deploying the application
- [**RUN_INTEGRATION_TESTS.md**](RUN_INTEGRATION_TESTS.md) - Running tests
- [**SECURITY.md**](SECURITY.md) - Security practices and secret management

### API Reference
- [**API_DOCUMENTATION.md**](API_DOCUMENTATION.md) - Complete API endpoint documentation

### Tools & Observability
- [**OBSERVABILITY_AND_SECURITY_TOOLS.md**](OBSERVABILITY_AND_SECURITY_TOOLS.md) - Using Spotless, Snyk, and OpenTelemetry

---

## 📚 Documentation by Use Case

### I want to...

#### Get the application running quickly
1. Start with [00_START_HERE.txt](00_START_HERE.txt)
2. Follow [QUICKSTART.md](QUICKSTART.md)
3. Reference [QUICK_REFERENCE.md](QUICK_REFERENCE.md) for common commands

#### Understand the architecture
1. Read [ARCHITECTURE_GUIDE.md](ARCHITECTURE_GUIDE.md)
2. Review [IMPLEMENTATION_NOTES.md](IMPLEMENTATION_NOTES.md)
3. Explore the code in `src/main/java/com/reconcile/`

#### Deploy to production
1. Follow [BUILD_AND_DEPLOY.md](BUILD_AND_DEPLOY.md)
2. Review [SECURITY.md](SECURITY.md) for secrets management
3. Check [OBSERVABILITY_AND_SECURITY_TOOLS.md](OBSERVABILITY_AND_SECURITY_TOOLS.md) for monitoring

#### Add a new reconciliation domain
1. Read [DOMAIN_MODEL_USAGE.md](DOMAIN_MODEL_USAGE.md)
2. Follow [FIELD_CONFIGURATION_GUIDE.md](FIELD_CONFIGURATION_GUIDE.md)
3. Consult [API_DOCUMENTATION.md](API_DOCUMENTATION.md) for endpoints

#### Use the API
1. Consult [API_DOCUMENTATION.md](API_DOCUMENTATION.md)
2. Try curl examples from [QUICK_REFERENCE.md](QUICK_REFERENCE.md)

#### Set up security properly
1. Read [SECURITY.md](SECURITY.md)
2. Follow the setup instructions for `.env`
3. Review environment variable requirements

#### Enable monitoring and observability
1. Follow [OBSERVABILITY_AND_SECURITY_TOOLS.md](OBSERVABILITY_AND_SECURITY_TOOLS.md)
2. Start the observability stack
3. Access Jaeger, Prometheus, and Grafana dashboards

#### Run tests
1. See [RUN_INTEGRATION_TESTS.md](RUN_INTEGRATION_TESTS.md)
2. Use provided test commands
3. Review test results

---

## 📁 Folder Structure

```
/doc                           # This folder - committed documentation
├── 00_START_HERE.txt
├── API_DOCUMENTATION.md
├── ARCHITECTURE_GUIDE.md
├── BUILD_AND_DEPLOY.md
├── DOMAIN_MODEL_USAGE.md
├── FIELD_CONFIGURATION_GUIDE.md
├── IMPLEMENTATION_NOTES.md
├── OBSERVABILITY_AND_SECURITY_TOOLS.md
├── QUICKSTART.md
├── QUICK_REFERENCE.md
├── RUN_INTEGRATION_TESTS.md
├── SECURITY.md
└── README.md (this file)

/docignore                     # Not committed - reference/historical docs
├── COMPOSITE_ID_IMPLEMENTATION.md
├── DELIVERY_SUMMARY.md
├── DOCUMENTATION_INDEX.md
├── INTEGRATION_TESTS.md
├── PROJECT_STRUCTURE.md
├── PROJECT_SUMMARY.md
├── REFACTORING_COMPLETE.md
├── REFACTORING_SUMMARY.md
├── SECURITY_AUDIT_SUMMARY.md
└── TOOLS_INTEGRATION_SUMMARY.md

/src                           # Source code
├── main/java/com/reconcile/
└── test/java/com/reconcile/

/sql                           # Database initialization scripts
├── source-db-init.sql
└── target-db-init.sql
```

---

## 🔍 Document Details

| Document | Purpose | Audience | Type |
|----------|---------|----------|------|
| 00_START_HERE.txt | Project overview | Everyone | Reference |
| API_DOCUMENTATION.md | API endpoints | Developers/Integration | Reference |
| ARCHITECTURE_GUIDE.md | System design | Developers | Educational |
| BUILD_AND_DEPLOY.md | Deploy instructions | DevOps/Developers | Guide |
| DOMAIN_MODEL_USAGE.md | Domain concepts | Developers | Educational |
| FIELD_CONFIGURATION_GUIDE.md | Field setup | DevOps/Developers | Guide |
| IMPLEMENTATION_NOTES.md | Implementation details | Developers | Reference |
| OBSERVABILITY_AND_SECURITY_TOOLS.md | Tools setup | DevOps/Developers | Guide |
| QUICKSTART.md | Fast setup | Everyone | Guide |
| QUICK_REFERENCE.md | Commands & examples | Everyone | Reference |
| RUN_INTEGRATION_TESTS.md | Testing procedures | Developers | Guide |
| SECURITY.md | Secrets & security | Everyone | Guide |

---

## 🔗 Key Links

- **Main README**: [../../README.md](../../README.md)
- **Environment Setup**: See [SECURITY.md](SECURITY.md) for `.env` setup
- **API Reference**: [API_DOCUMENTATION.md](API_DOCUMENTATION.md)
- **Quick Commands**: [QUICK_REFERENCE.md](QUICK_REFERENCE.md)

---

## 📝 Contributing

When adding documentation:
1. If it's essential for developers/deployment → place in `/doc`
2. If it's reference/historical → place in `/docignore`
3. Update this README.md with reference to new docs
4. Keep files concise and focused on a single topic

---

## 🚀 Quick Start Path

For the fastest path to a running system:
```
1. Read: 00_START_HERE.txt
2. Read: QUICKSTART.md
3. Run: Commands from doc/BUILD_AND_DEPLOY.md
4. Access: http://localhost:8080/api/reconcile/health
```

---

## ❓ Need Help?

- **API questions** → [API_DOCUMENTATION.md](API_DOCUMENTATION.md)
- **Architecture questions** → [ARCHITECTURE_GUIDE.md](ARCHITECTURE_GUIDE.md)
- **Deployment questions** → [BUILD_AND_DEPLOY.md](BUILD_AND_DEPLOY.md)
- **Security questions** → [SECURITY.md](SECURITY.md)
- **Quick commands** → [QUICK_REFERENCE.md](QUICK_REFERENCE.md)
