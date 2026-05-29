# Secrets Management Guide

This guide explains how to securely manage credentials and sensitive data in the FlossWare Platform.

## ⚠️ Security Warning

**NEVER commit credentials to version control.**

Plaintext passwords, API keys, and tokens in configuration files create severe security risks:
- Credentials exposed in git history (even after deletion)
- Unauthorized access if repository is compromised
- Compliance violations (PCI-DSS, HIPAA, SOC 2)
- Credential rotation requires code changes

## Environment Variable Substitution

The platform automatically resolves environment variable references in application descriptor properties, allowing you to externalize sensitive values.

### Syntax

The platform supports two environment variable formats:

#### 1. Braced Format with Optional Default

```yaml
${VARIABLE_NAME}              # Required variable
${VARIABLE_NAME:default}      # With default value
```

#### 2. Simple Format

```yaml
$VARIABLE_NAME                # No default support
```

### Example: Application Descriptor

**Before** (Insecure - plaintext credentials):

```yaml
applicationId: my-app
mainClass: com.example.MyApp
properties:
  database.url: jdbc:postgresql://localhost:5432/mydb
  database.username: admin
  database.password: SuperSecret123!        # ⚠️ DANGER: Plaintext password
  api.key: sk_live_abc123xyz                # ⚠️ DANGER: Plaintext API key
```

**After** (Secure - environment variables):

```yaml
applicationId: my-app
mainClass: com.example.MyApp
properties:
  database.url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME}
  database.username: ${DB_USERNAME}
  database.password: ${DB_PASSWORD}         # ✅ Resolved at runtime
  api.key: ${API_KEY}                       # ✅ Resolved at runtime
```

### Setting Environment Variables

#### Linux/macOS

```bash
# Export for current session
export DB_PASSWORD="SuperSecret123!"
export API_KEY="sk_live_abc123xyz"

# Add to ~/.bashrc or ~/.zshrc for persistence
echo 'export DB_PASSWORD="SuperSecret123!"' >> ~/.bashrc
echo 'export API_KEY="sk_live_abc123xyz"' >> ~/.bashrc
```

#### Windows (PowerShell)

```powershell
# Set for current session
$env:DB_PASSWORD = "SuperSecret123!"
$env:API_KEY = "sk_live_abc123xyz"

# Set permanently
[System.Environment]::SetEnvironmentVariable('DB_PASSWORD', 'SuperSecret123!', 'User')
[System.Environment]::SetEnvironmentVariable('API_KEY', 'sk_live_abc123xyz', 'User')
```

#### Docker

```yaml
# docker-compose.yml
services:
  platform:
    image: flossware/platform-java:latest
    environment:
      - DB_PASSWORD=SuperSecret123!
      - API_KEY=sk_live_abc123xyz
    # Or use env_file:
    env_file:
      - .env.production
```

```bash
# .env.production (DO NOT commit this file)
DB_PASSWORD=SuperSecret123!
API_KEY=sk_live_abc123xyz
```

#### Kubernetes

```yaml
# Secret definition
apiVersion: v1
kind: Secret
metadata:
  name: platform-secrets
type: Opaque
stringData:
  db-password: SuperSecret123!
  api-key: sk_live_abc123xyz

---
# Deployment using secrets
apiVersion: apps/v1
kind: Deployment
metadata:
  name: platform
spec:
  template:
    spec:
      containers:
      - name: platform
        image: flossware/platform-java:latest
        env:
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: platform-secrets
              key: db-password
        - name: API_KEY
          valueFrom:
            secretKeyRef:
              name: platform-secrets
              key: api-key
```

### Default Values

Use default values for optional configuration (not secrets):

```yaml
properties:
  # Good: Optional configuration with sensible default
  cache.ttl: ${CACHE_TTL:3600}
  log.level: ${LOG_LEVEL:INFO}
  
  # Bad: Secrets should never have defaults
  api.key: ${API_KEY:insecure_default}     # ⚠️ DON'T DO THIS
```

**Rule**: Never use defaults for credentials. If a credential is missing, the application should fail fast.

## Advanced: External Secrets Managers

For production environments, use dedicated secrets management systems:

### HashiCorp Vault Integration

```yaml
# Future implementation (platform-config-vault module)
applicationId: my-app
mainClass: com.example.MyApp
properties:
  database.password: vault://secret/data/platform/db-credentials#password
  api.key: vault://secret/data/platform/api-keys#live
```

Configuration:

```bash
export VAULT_ADDR=https://vault.example.com:8200
export VAULT_TOKEN=s.abc123xyz
```

### AWS Secrets Manager

```yaml
# Future implementation
properties:
  database.password: aws-secrets://platform/db-credentials#password
  api.key: aws-secrets://platform/api-keys#live
```

Configuration:

```bash
export AWS_REGION=us-east-1
# Use IAM role or:
export AWS_ACCESS_KEY_ID=AKIAIOSFODNN7EXAMPLE
export AWS_SECRET_ACCESS_KEY=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
```

### Azure Key Vault

```yaml
# Future implementation
properties:
  database.password: azure-keyvault://platform-vault/db-password
  api.key: azure-keyvault://platform-vault/api-key
```

## Best Practices

### 1. Use Environment Variables for All Secrets

✅ **Do**:
```yaml
properties:
  api.key: ${API_KEY}
  database.password: ${DB_PASSWORD}
  jwt.secret: ${JWT_SECRET}
```

❌ **Don't**:
```yaml
properties:
  api.key: sk_live_hardcoded_key       # Never hardcode
  database.password: admin123          # Never hardcode
```

### 2. Validate Required Secrets at Startup

The platform will log warnings if environment variables are not found. Monitor logs:

```
WARN  EnvironmentVariableResolver - Environment variable DB_PASSWORD not found and no default provided
```

Consider failing fast in production if critical secrets are missing.

### 3. Use Descriptive Variable Names

✅ **Good**:
```bash
export DB_PASSWORD="..."
export STRIPE_API_KEY="..."
export SENDGRID_API_KEY="..."
```

❌ **Bad**:
```bash
export PWD="..."                 # Ambiguous
export KEY="..."                 # Which key?
export SECRET="..."              # Which secret?
```

### 4. Separate Environments

Use different variable values per environment:

```bash
# development.env
DB_PASSWORD=dev_password
API_KEY=sk_test_abc123

# production.env
DB_PASSWORD=prod_strong_password
API_KEY=sk_live_xyz789
```

### 5. Rotate Credentials Regularly

With environment variables, rotation is simple:

```bash
# Update environment variable
export API_KEY="sk_live_new_rotated_key"

# Restart application (picks up new value)
systemctl restart platform
```

### 6. Use .gitignore for Environment Files

```gitignore
# .gitignore
.env
.env.*
*.env
secrets/
credentials/
```

### 7. Audit Secret Access

Monitor logs for secret resolution:

```
DEBUG EnvironmentVariableResolver - Resolved DB_PASSWORD from environment: ***
DEBUG EnvironmentVariableResolver - Resolved API_KEY from environment: sk_***
```

The resolver automatically masks sensitive values in logs (shows first 3 characters only).

## Migration Guide

### Step 1: Identify Hardcoded Credentials

Search for plaintext credentials:

```bash
# Find potential credentials in YAML files
grep -r "password\|api.*key\|secret\|token" descriptors/ --include="*.yaml"

# Check for common patterns
grep -r "sk_live_\|sk_test_" descriptors/ --include="*.yaml"
```

### Step 2: Replace with Environment Variables

For each credential found:

1. Choose a descriptive environment variable name (e.g., `DB_PASSWORD`)
2. Replace the value with `${VARIABLE_NAME}`
3. Document the required variable

**Before**:
```yaml
properties:
  database.password: admin123
```

**After**:
```yaml
properties:
  database.password: ${DB_PASSWORD}
```

### Step 3: Set Environment Variables

Create a secrets file (DO NOT commit):

```bash
# secrets.env
export DB_PASSWORD="admin123"
export API_KEY="sk_live_abc123"
export JWT_SECRET="my_secret_key"
```

Source before starting:

```bash
source secrets.env
java -jar platform-launcher.jar
```

### Step 4: Update Documentation

Document required environment variables in your project README:

```markdown
## Required Environment Variables

- `DB_PASSWORD` - Database password
- `API_KEY` - Stripe API key for payment processing
- `JWT_SECRET` - Secret for signing JWT tokens
```

### Step 5: Verify

Test that resolution works:

```bash
# Set test value
export TEST_SECRET="test123"

# Create descriptor with variable
echo "properties:
  test.value: \${TEST_SECRET}" > test-descriptor.yaml

# Deploy and verify
# The platform should log: "Resolved TEST_SECRET from environment: tes***"
```

## Troubleshooting

### Variable Not Resolved

**Problem**: Property value shows `${VAR_NAME}` instead of actual value

**Solutions**:
1. Check variable is set: `echo $VAR_NAME`
2. Export variable: `export VAR_NAME="value"`
3. Check variable name spelling (case-sensitive)
4. Verify no typos in descriptor: `${VAR_NAME}` not `${VAR_NAEM}`

### Special Characters in Values

**Problem**: Passwords with special characters not working

**Solution**: Quote values:

```bash
# Correct
export DB_PASSWORD='P@$$w0rd!#$'

# Also correct
export DB_PASSWORD="P@\$\$w0rd!#\$"
```

### Variable Not Persisting

**Problem**: Variable disappears after terminal closes

**Solutions**:
1. Add to shell profile: `echo 'export VAR="value"' >> ~/.bashrc`
2. Use systemd environment files: `/etc/environment`
3. Use Docker env files: `env_file: .env.production`
4. Use Kubernetes secrets (shown above)

## Security Checklist

Before deploying to production:

- [ ] No hardcoded credentials in YAML files
- [ ] All secrets use environment variables
- [ ] `.env` files are in `.gitignore`
- [ ] Credentials are not in git history
- [ ] Environment variables are documented
- [ ] Separate secrets per environment (dev/staging/prod)
- [ ] Secrets have appropriate permissions (e.g., `chmod 600 secrets.env`)
- [ ] Audit logging is enabled
- [ ] Credential rotation process is documented

## Future Enhancements

The following secrets management features are planned:

- **Vault Integration** (Issue #323): Native HashiCorp Vault support
- **AWS Secrets Manager**: AWS-native secrets retrieval
- **Azure Key Vault**: Azure-native secrets retrieval
- **GCP Secret Manager**: GCP-native secrets retrieval
- **Automatic Rotation**: Periodic credential refresh without restart
- **Encryption at Rest**: Encrypt cached credentials in memory

## References

- [OWASP Secrets Management Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Secrets_Management_Cheat_Sheet.html)
- [12-Factor App: Config](https://12factor.net/config)
- [HashiCorp Vault Documentation](https://www.vaultproject.io/docs)
- [Kubernetes Secrets](https://kubernetes.io/docs/concepts/configuration/secret/)
- [Docker Secrets](https://docs.docker.com/engine/swarm/secrets/)
