# Claude Code Workflows - Quick Reference Card

## 🔍 Auto-Review: Code Quality Scanning

### Basic Usage
```
Review the code for security and quality issues
```

### Focused Scans
| What You Want | What To Say |
|---------------|-------------|
| Security only | `Review code for security vulnerabilities` |
| Quality only | `Check code quality and complexity` |
| Test coverage | `Analyze test coverage gaps` |
| Dependencies | `Scan dependencies for CVEs` |
| Specific module | `Review platform-security module for issues` |
| Recent changes | `Review files changed in last commit` |

### Severity Control
```
Review code but only create issues for critical and high severity
Review code and auto-fix all P0 critical issues
```

### Scheduling
```
Set up daily code review at 9am
Configure weekly comprehensive scan every Monday
```

---

## 🔧 Auto-Resolve: GitHub Issue Resolution

### Basic Usage
```
Pick and resolve a GitHub issue
Resolve the next open issue
```

### Priority Selection
| What You Want | What To Say |
|---------------|-------------|
| Security fix | `Resolve the highest priority security issue` |
| Bug fix | `Pick and fix a bug from the issue tracker` |
| Enhancement | `Resolve an enhancement issue` |
| Specific issue | `Fix GitHub issue #329` |
| Label-based | `Resolve an issue labeled 'good-first-issue'` |

### Continuous Mode
```
Keep resolving GitHub issues until I tell you to stop
Resolve 5 issues then stop
Work through security backlog for 1 hour
```

---

## 🔄 Combined Workflows

### Daily Maintenance
```
Review yesterday's changes and fix any critical issues
```

### Sprint Cleanup
```
Review code, create issues, then resolve quick wins
```

### Security Hardening
```
Security audit the codebase and fix all vulnerabilities
```

### Tech Debt Sprint
```
Review for code quality and resolve top 5 issues
```

---

## ⚙️ Configuration

### Review Config (`.claude/review-config.yaml`)
```yaml
review:
  schedule: "0 9 * * *"
  severity_threshold: high
  auto_fix: true
  dimensions:
    security: true
    quality: true
    coverage: true
```

### Resolve Config (`.claude/resolve-config.yaml`)
```yaml
resolve:
  priority_labels: [security, bug, enhancement]
  skip_labels: [wontfix, blocked]
  require_tests: true
```

---

## 📊 Review Dimensions

| Dimension | What It Checks |
|-----------|----------------|
| **Security** | OWASP Top 10, path traversal, injection, CVEs |
| **Quality** | Complexity, duplication, code smells, long methods |
| **Coverage** | Untested paths, missing edge cases, gaps |
| **Documentation** | Missing JavaDoc, TODOs, outdated docs |
| **Dependencies** | Outdated versions, vulnerabilities |

---

## 🎯 Severity Levels

| Level | Type | Action |
|-------|------|--------|
| **P0** | Critical | Auto-fix + create issue |
| **P1** | High | Create issue, assign to team |
| **P2** | Medium | Create issue, backlog |
| **P3** | Low | Log only, no issue |

---

## ✅ Best Practices

### DO
- ✅ Run daily quick scans
- ✅ Run weekly deep scans  
- ✅ Review auto-fixes before merging
- ✅ Let Claude pick issues to resolve
- ✅ Track metrics over time

### DON'T
- ❌ Auto-fix in production without review
- ❌ Ignore P2/P3 findings forever
- ❌ Force Claude to fix unfeasible issues
- ❌ Skip test verification

---

## 🚀 Quick Start

1. **First Review:**
   ```
   Review the code for security issues
   ```

2. **First Resolution:**
   ```
   Resolve a GitHub issue
   ```

3. **Set Up Automation:**
   ```
   Configure daily code review at 9am
   ```

4. **Monitor & Iterate:**
   ```
   Show code quality trends for last month
   ```

---

## 🔑 Key Insight

**No slash commands needed!**

Instead of:
- ~~`/auto-review`~~ → `"Review the code"`
- ~~`/auto-resolve`~~ → `"Resolve an issue"`
- ~~`/auto-review-loop`~~ → `"Set up daily reviews"`

**Just ask in plain English - the capabilities are built-in!**

---

**Full Guide**: `CLAUDE_CODE_WORKFLOWS.md`  
**Detailed Setup**: `.claude/CONTINUOUS_REVIEW_GUIDE.md`  
**Skills Reference**: `.claude/SKILLS_README.md`
