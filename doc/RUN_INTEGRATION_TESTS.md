# Integration Test Script

A standalone shell script for running the Oracle → PostgreSQL reconciliation integration tests with full Docker support.

## Script Location

```bash
./run-integration-tests.sh
```

## Features

✅ **Pre-flight Checks**
- Verifies Docker Desktop is running
- Checks Docker socket accessibility (`/var/run/docker.sock`)
- Validates Java installation
- Confirms Docker daemon connectivity

✅ **Automatic Compilation**
- Cleans build artifacts
- Compiles main source code
- Compiles test code
- Error reporting on compilation failures

✅ **Docker Configuration**
- Auto-detects Docker socket location
- Sets `DOCKER_HOST` environment variable
- Configures Testcontainers for macOS Docker Desktop
- Supports manual Docker socket override

✅ **Test Execution**
- Runs integration tests with proper Docker setup
- Captures test output to `/tmp/integration-test.log`
- Displays test results with color-coded output
- Shows only relevant output lines

✅ **Help & Cleanup**
- Optional `--clean` flag for container cleanup
- Optional `--debug` flag for detailed output
- Optional `--compile-only` flag to compile without running tests

## Usage

### Basic Execution (Recommended)

```bash
cd /Users/philipthomas/repo/reconciliation-service
./run-integration-tests.sh
```

### With Debug Output

Shows detailed classpath and configuration:

```bash
./run-integration-tests.sh --debug
```

### Compile Only

Compile code without running tests:

```bash
./run-integration-tests.sh --compile-only
```

### Clean Docker Containers

Remove leftover test containers and run tests:

```bash
./run-integration-tests.sh --clean
```

## Test Output Example

```
╔════════════════════════════════════════════════════════════════╗
║   Integration Test Runner - Oracle → PostgreSQL Reconciliation  ║
╚════════════════════════════════════════════════════════════════╝

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Pre-flight Checks
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Checking Docker Setup
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

✓ Found: docker
✓ Found: java
✓ Docker daemon accessible
✓ Docker socket accessible at /var/run/docker.sock

Docker Configuration:
   Server Version: 29.3.1
   Storage Driver: overlayfs
   Runtimes: io.containerd.runc.v2 runc

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Compiling Project
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

✓ Compilation successful

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Building Classpath
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

✓ Classpath configured

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Running Integration Tests
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Test Configuration:
  Docker Host: unix:///var/run/docker.sock
  Project Root: /Users/philipthomas/repo/reconciliation-service
  Java Version: openjdk version "21.0.6" 2025-01-21 LTS

Starting Oracle container (gvenzl/oracle-xe:21-slim-faststart)...
Starting PostgreSQL container (postgres:15-alpine)...

Executing integration tests via Gradle...

[Test execution logs...]

✓ Integration tests passed
```

## Environment Variables

The script automatically sets:

- `DOCKER_HOST=unix:///var/run/docker.sock` - Docker socket location
- `TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock` - Testcontainers override

You can manually override:

```bash
export DOCKER_HOST=unix:///custom/path/to/docker.sock
./run-integration-tests.sh
```

## Troubleshooting

### Docker Socket Not Found

```bash
❌ Docker socket not found at /var/run/docker.sock
```

**Solution:** Check symbolic link on macOS:
```bash
ls -la /var/run/docker.sock
# Should show: /var/run/docker.sock -> /Users/USERNAME/.docker/run/docker.sock
```

### Docker Daemon Not Running

```bash
❌ Docker daemon not accessible
```

**Solution:** Start Docker Desktop or restart it:
```bash
open /Applications/Docker.app
```

### Compilation Failures

```bash
❌ Compilation failed
```

**Solution:** Run with debug output:
```bash
./run-integration-tests.sh --debug
```

### Container Image Pull Timeout

First run may take several minutes downloading:
- `gvenzl/oracle-xe:21-slim-faststart` (~2GB)
- `postgres:15-alpine` (~100MB)

Ensure sufficient disk space and network connectivity. Check Docker Desktop memory allocation (minimum 4GB recommended).

## Test Results

After execution, detailed test reports are available at:

- **HTML Report:** `build/reports/tests/integrationTest/index.html`
- **Test Results XML:** `build/test-results/integrationTest/`
- **Script Log:** `/tmp/integration-test.log`

## Known Issues

### Testcontainers ↔ Gradle ↔ Docker Desktop

Due to macOS Docker Desktop architecture, Testcontainers may have trouble connecting to Docker when running through Gradle's worker processes. The script works around this by:

1. Pre-validating Docker accessibility
2. Setting explicit `DOCKER_HOST` variable
3. Configuring Testcontainers properties
4. Providing fallback mechanisms

If tests still fail with Docker connection errors, try:

```bash
# Manual environment setup
export DOCKER_HOST=unix:///var/run/docker.sock
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock

# Then run
./run-integration-tests.sh
```

## Advanced Usage

### Run Only Compilation

```bash
./run-integration-tests.sh --compile-only
```

### Clean Containers Before Tests

```bash
./run-integration-tests.sh --clean
```

### Debug with Full Classpath Output

```bash
./run-integration-tests.sh --debug
```

This shows the full classpath entries and detailed configuration.

## Integration with CI/CD

For CI/CD pipelines (GitHub Actions, GitLab CI, etc.):

```yaml
# Example: GitHub Actions
- name: Run Integration Tests
  run: |
    cd reconciliation-service
    chmod +x run-integration-tests.sh
    ./run-integration-tests.sh --debug
  env:
    DOCKER_HOST: unix:///run/docker.sock
```

## Script Source Code

Location: [`run-integration-tests.sh`](run-integration-tests.sh)

The script includes:
- Docker validation functions
- Compilation orchestration
- Classpath extraction
- Test execution with Docker setup
- Container cleanup utilities
- Color-coded console output

## Related Documentation

- [INTEGRATION_TESTS.md](INTEGRATION_TESTS.md) - Full test documentation
- [PROJECT_STRUCTURE.md](../PROJECT_STRUCTURE.md) - Project layout
- [ARCHITECTURE_GUIDE.md](../ARCHITECTURE_GUIDE.md) - System design
