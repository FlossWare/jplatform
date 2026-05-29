# Continuous Code Review Guide

## Overview

Automated code reviews run on a schedule to continuously monitor code quality, security vulnerabilities, and technical debt.

## Setup

### Option 1: Daily Reviews (Recommended)

Run comprehensive code review every day at 9am:

\`\`\`bash
# Create cron job for daily reviews
/loop "0 9 * * *" "/auto-review --severity=high"
\`\`\`

### Option 2: Weekly Reviews

Run exhaustive review every Monday at 9am:

\`\`\`bash
# Create cron job for weekly reviews
/loop "0 9 * * 1" "/auto-review --severity=all --deep-scan"
\`\`\`

### Option 3: On-Commit Reviews

Trigger review on every commit via Git hook:

\`\`\`bash
# .git/hooks/post-commit
#!/bin/bash
claude-code --session=auto-review "/auto-review --quick"
\`\`\`

## Review Dimensions

Each review scans across multiple dimensions:

### 1. Security Vulnerabilities
- OWASP Top 10 checks
- Path traversal attempts
- SQL injection risks
- XSS vulnerabilities
- Insecure deserialization
- Known CVEs in dependencies

### 2. Code Quality
- Cyclomatic complexity
- Code duplication
- Long methods (>150 lines)
- God classes (>500 lines)
- Lack of cohesion
- Code smells

### 3. Test Coverage
- Uncovered code paths
- Missing edge case tests
- Untested error handling
- Integration test gaps
- Missing security tests

### 4. Documentation
- Missing JavaDoc on public APIs
- Outdated documentation
- TODO/FIXME audit
- Missing README sections

### 5. Dependencies
- Outdated versions
- Known vulnerabilities (CVE database)
- Unused dependencies
- License compatibility

## Severity Levels

Reviews create GitHub issues based on severity:

| Severity | Description | Action |
|----------|-------------|--------|
| **P0 (Critical)** | Security vulnerability, data loss risk | Auto-fix immediately + create issue |
| **P1 (High)** | Production blocker, major bug | Create issue, assign to team |
| **P2 (Medium)** | Technical debt, code smell | Create issue, backlog |
| **P3 (Low)** | Nice-to-have, optimization | Log only, no issue |

## Configuration

### Custom Review Rules

Create `.claude/review-config.yaml`:

\`\`\`yaml
review:
  schedule: "0 9 * * *"  # Daily at 9am
  severity_threshold: high  # Only P0/P1 issues
  auto_fix: true  # Auto-fix P0 issues
  
  dimensions:
    security:
      enabled: true
      checks:
        - owasp-top-10
        - dependency-vulnerabilities
        - path-traversal
        
    quality:
      enabled: true
      thresholds:
        max_method_length: 150
        max_class_length: 500
        max_cyclomatic_complexity: 15
        
    coverage:
      enabled: true
      minimum: 60  # Fail if below 60%
      
    documentation:
      enabled: true
      require_javadoc: public  # public methods only
      
  exclusions:
    paths:
      - "*/test/*"
      - "*/generated/*"
    files:
      - "*.generated.java"
\`\`\`

### GitHub Integration

Auto-create issues for findings:

\`\`\`yaml
github:
  create_issues: true
  labels:
    - code-review
    - automated
  assign_to: "@team-lead"
  milestone: "Tech Debt Sprint"
\`\`\`

## Review Reports

Reviews generate:

1. **Summary Report** - `.claude/review-output/summary-YYYY-MM-DD.md`
2. **Detailed Findings** - `.claude/review-output/findings-YYYY-MM-DD.json`
3. **Trend Analysis** - `.claude/review-output/trends.csv`

### Example Summary Report

\`\`\`markdown
# Code Review Summary - 2026-05-29

## Overview
- **Files Scanned**: 153
- **Issues Found**: 12
- **Auto-Fixed**: 3

## By Severity
- **P0 (Critical)**: 1 - Path traversal (auto-fixed)
- **P1 (High)**: 4 - Missing input validation
- **P2 (Medium)**: 5 - Code duplication
- **P3 (Low)**: 2 - Missing JavaDoc

## By Dimension
- **Security**: 5 issues
- **Quality**: 4 issues
- **Coverage**: 2 issues
- **Documentation**: 1 issue

## Top Issues

### #1 [P0] Path Traversal in NativeProcessLauncher
**Status**: Auto-fixed
**File**: platform-core/src/main/java/.../NativeProcessLauncher.java:131
**Fix**: Added path validation

### #2 [P1] Missing Input Validation in ApiAuthFilter
**Status**: Issue created (#340)
**File**: platform-rest-api/src/main/java/.../ApiAuthFilter.java:87
**Recommendation**: Add null check before authentication

...
\`\`\`

## Best Practices

### 1. **Review Scheduling**
- **Daily**: Quick scans (5-10 min) for recent changes
- **Weekly**: Deep scans (30-60 min) for comprehensive coverage
- **On-Commit**: Lightweight checks (1-2 min) for immediate feedback

### 2. **Issue Triage**
- Review auto-created issues daily
- Close false positives immediately
- Assign P0/P1 issues to sprint
- Backlog P2/P3 for tech debt sprints

### 3. **Auto-Fix Guidelines**
- Only auto-fix P0 issues with high confidence
- Always create backup branch before auto-fix
- Review auto-fixes in next PR
- Disable auto-fix for critical systems

### 4. **Metrics Tracking**
- Track issues found/fixed over time
- Monitor coverage trends
- Measure mean-time-to-fix by severity
- Report on security posture monthly

## Troubleshooting

### Review Taking Too Long

Reduce scope:
\`\`\`yaml
review:
  scope: changed  # Only review changed files
  max_files: 50   # Limit to 50 files per run
\`\`\`

### Too Many False Positives

Tune sensitivity:
\`\`\`yaml
review:
  sensitivity: low  # Reduce false positives
  confidence_threshold: 0.8  # Higher confidence required
\`\`\`

### Missing Issues

Increase depth:
\`\`\`yaml
review:
  deep_scan: true
  check_transitive_deps: true
  analyze_dead_code: true
\`\`\`

## Integration with CI/CD

### GitHub Actions

\`\`\`yaml
# .github/workflows/code-review.yml
name: Automated Code Review

on:
  schedule:
    - cron: '0 9 * * *'  # Daily at 9am
  push:
    branches: [main, develop]

jobs:
  review:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Run Claude Code Review
        run: |
          claude-code "/auto-review --ci-mode"
          
      - name: Upload Report
        uses: actions/upload-artifact@v4
        with:
          name: code-review-report
          path: .claude/review-output/
\`\`\`

## Metrics Dashboard

Track review metrics over time:

\`\`\`bash
# Generate metrics dashboard
claude-code "/auto-review --metrics-dashboard"
\`\`\`

Creates HTML dashboard at `.claude/review-output/dashboard.html`:
- Issues found/fixed trend
- Coverage trend
- Security posture score
- Top file hotspots
- Technical debt accumulation

## Next Steps

1. **Choose schedule** - Daily, weekly, or on-commit
2. **Configure rules** - Create `.claude/review-config.yaml`
3. **Test run** - Run `/auto-review --dry-run`
4. **Enable automation** - Set up cron job or CI/CD integration
5. **Monitor results** - Review reports daily
6. **Iterate** - Tune configuration based on findings

---

**Last Updated**: May 29, 2026  
**Related Skills**: `/auto-review`, `/auto-review-loop`  
**Issue**: #338
