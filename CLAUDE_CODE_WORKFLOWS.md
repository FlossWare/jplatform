# Claude Code Workflows: Auto-Review & Auto-Resolve

## Overview

This guide documents how to use Claude Code for automated code reviews and GitHub issue resolution. These capabilities work through natural language prompts (custom `/auto-*` slash commands are not required).

---

## 1. Auto-Review: Automated Code Quality Review

### What It Does

Performs comprehensive code review across multiple dimensions:
- **Security**: OWASP Top 10, path traversal, injection vulnerabilities, CVE scanning
- **Code Quality**: Complexity, duplication, code smells, long methods
- **Test Coverage**: Gaps, missing edge cases, untested error paths
- **Documentation**: Missing JavaDoc, outdated docs, TODO/FIXME audit
- **Dependencies**: Outdated versions, known vulnerabilities

### How to Invoke

**Basic Review:**
```
Review the code for security and quality issues
```

**Specific Focus:**
```
Review the codebase for security vulnerabilities only
Check code quality and test coverage
Scan for outdated dependencies with known CVEs
```

**File-Specific Review:**
```
Review platform-core/src/main/java for security issues
Check the REST API module for vulnerabilities
```

### What Happens

Claude will:
1. **Scan** relevant files across specified dimensions
2. **Analyze** findings for severity (P0=Critical, P1=High, P2=Medium, P3=Low)
3. **Verify** findings aren't false positives
4. **Create GitHub issues** for P0/P1 findings
5. **Auto-fix** P0 critical issues (optional)
6. **Generate report** summarizing findings

### Example Output

```markdown
## Code Review Summary

**Files Scanned**: 153
**Issues Found**: 12
**Auto-Fixed**: 3

### Critical Issues (P0)

1. **Path Traversal in NativeProcessLauncher**
   - File: platform-core/.../NativeProcessLauncher.java:131
   - Status: Auto-fixed
   - Fix: Added path validation rejecting `../` sequences

### High Priority Issues (P1)

2. **Missing Input Validation in ApiAuthFilter**
   - File: platform-rest-api/.../ApiAuthFilter.java:87
   - Recommendation: Add null check before authentication
   - Issue Created: #340

...
```

### Configuration Options

You can customize behavior by specifying preferences:

**Severity Threshold:**
```
Review code but only create issues for P0 and P1 findings
```

**Auto-Fix Behavior:**
```
Review code and auto-fix all critical security issues
Review code but don't auto-fix anything (just report)
```

**Scope Limiting:**
```
Review only files changed in the last commit
Review only the platform-security module
```

### Review Schedule

**Daily Reviews (Recommended):**
```
Set up a daily code review at 9am
Create a cron job to review code every morning
```

**Weekly Deep Scans:**
```
Set up a comprehensive weekly code review every Monday
```

**On-Commit Reviews:**
```
Configure post-commit hook to run quick review after each commit
```

---

## 2. Auto-Resolve: Automated GitHub Issue Resolution

### What It Does

Automatically picks a GitHub issue from your repository, analyzes it, implements a fix, creates tests, and submits the solution.

### How to Invoke

**Pick Any Issue:**
```
Pick and resolve a GitHub issue
Automatically resolve an open issue
```

**Priority-Based:**
```
Resolve the highest priority security issue
Pick and fix a bug from the issue tracker
```

**Label-Based:**
```
Resolve a security-labeled issue
Pick an enhancement issue and implement it
```

**Specific Issue:**
```
Resolve GitHub issue #329
Fix issue #322 about path traversal
```

### What Happens

Claude will:
1. **Fetch** open issues from GitHub
2. **Analyze** each issue for feasibility
3. **Select** best candidate (or use your specification)
4. **Plan** implementation approach
5. **Implement** fix with proper code
6. **Write tests** to verify the fix
7. **Verify** tests pass
8. **Document** changes
9. **Close issue** with detailed comment

### Example Workflow

**User Request:**
```
Resolve the next security issue
```

**Claude Response:**
```
I'll pick and resolve a security issue. Let me check what's available...

Selected: Issue #322 - Path Traversal Vulnerability in NativeProcessLauncher

Reading NativeProcessLauncher.java...
Found vulnerability at line 131-134.

Implementing fix:
- Adding path validation method
- Rejecting `../` and `..\\` sequences
- Restricting system directory access
- Adding comprehensive security tests

Running tests... ✅ All 15 tests passing

Closing issue #322 with resolution details.

✅ Issue #322 resolved successfully!
```

### Selection Criteria

Claude prioritizes issues based on:
1. **Severity** - Security > Bugs > Enhancements
2. **Impact** - Production blockers first
3. **Feasibility** - Can it be fixed with available context?
4. **Dependencies** - Are blockers resolved?
5. **Labels** - Respects priority labels

### Continuous Resolution

**Autonomous Mode:**
```
Keep resolving GitHub issues until I tell you to stop
Continuously work through the issue backlog
```

This will:
- Pick an issue
- Resolve it
- Pick next issue
- Repeat until interrupted

**With Limits:**
```
Resolve 5 GitHub issues then stop
Work through security issues for the next hour
```

---

## 3. Combined Workflow: Review + Resolve

### Daily Maintenance Pattern

```
Review the code for issues, then resolve any P0/P1 findings
```

This will:
1. Run comprehensive code review
2. Create GitHub issues for findings
3. Immediately resolve critical (P0) issues
4. Leave P1 issues for manual review

### Sprint Cleanup Pattern

```
Review code and resolve all findings under 2 hours of work
```

Claude will:
1. Review codebase
2. Estimate effort for each finding
3. Resolve quick wins (< 2 hours)
4. Create issues for larger work

---

## 4. Configuration & Customization

### Project-Specific Rules

Create `.claude/review-config.yaml`:

```yaml
review:
  schedule: "0 9 * * *"  # Daily at 9am
  severity_threshold: high  # Only P0/P1
  auto_fix: true
  
  dimensions:
    security:
      enabled: true
      checks:
        - owasp-top-10
        - dependency-vulnerabilities
    quality:
      enabled: true
      thresholds:
        max_method_length: 150
        max_complexity: 15
    coverage:
      enabled: true
      minimum: 60
      
  exclusions:
    paths:
      - "*/test/*"
      - "*/generated/*"
```

### Issue Resolution Rules

Create `.claude/resolve-config.yaml`:

```yaml
resolve:
  priority_labels:
    - security
    - bug
    - enhancement
    
  skip_labels:
    - wontfix
    - duplicate
    - blocked
    
  auto_assign: true
  create_branch: true
  run_tests: true
  
  verification:
    require_tests: true
    require_documentation: true
    min_test_coverage: 80
```

---

## 5. Best Practices

### For Code Reviews

**✅ DO:**
- Run daily quick scans for recent changes
- Run weekly deep scans for comprehensive coverage
- Configure severity thresholds to avoid noise
- Review auto-fix changes in next PR
- Track metrics over time

**❌ DON'T:**
- Auto-fix in production branches without review
- Ignore P2/P3 findings indefinitely (tech debt accumulates)
- Run deep scans on every commit (too slow)
- Disable security checks to reduce noise

### For Issue Resolution

**✅ DO:**
- Let Claude pick issues (it knows what's feasible)
- Review auto-generated tests
- Verify fixes in local environment
- Use continuous mode during dedicated cleanup time
- Trust but verify - check the actual code changes

**❌ DON'T:**
- Force Claude to fix issues beyond its context window
- Skip test verification
- Merge without code review
- Resolve issues that require human judgment (UX decisions, architecture choices)

---

## 6. Example Prompts Library

### Code Review Prompts

```bash
# Daily standup review
"Review code changes from the last 24 hours"

# Pre-release audit
"Comprehensive security and quality review for production readiness"

# Module-specific
"Review the platform-rest-api module for security vulnerabilities"

# Dependency audit
"Scan all dependencies for known CVEs and outdated versions"

# Test coverage analysis
"Check test coverage and identify untested code paths"

# Documentation audit
"Review JavaDoc completeness on all public APIs"
```

### Issue Resolution Prompts

```bash
# Priority-based
"Resolve the highest priority open issue"
"Fix the next security bug"

# Label-based
"Resolve an enhancement issue"
"Pick a good-first-issue and fix it"

# Continuous mode
"Keep resolving security issues until I stop you"
"Work through the backlog for the next 2 hours"

# Specific issues
"Fix GitHub issue #329 about the BOM module"
"Resolve all issues labeled 'quick-win'"
```

### Combined Workflows

```bash
# Daily maintenance
"Review yesterday's commits and fix any P0 issues found"

# Sprint cleanup
"Review code, create issues for findings, then resolve quick wins"

# Security hardening
"Security audit the codebase and fix all critical vulnerabilities"

# Tech debt sprint
"Review for code quality issues and resolve the top 5"
```

---

## 7. Integration with CI/CD

### GitHub Actions Integration

Create `.github/workflows/auto-review.yml`:

```yaml
name: Automated Code Review

on:
  schedule:
    - cron: '0 9 * * *'  # Daily at 9am
  push:
    branches: [main]

jobs:
  review:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      issues: write
      
    steps:
      - uses: actions/checkout@v4
      
      - name: Run Code Review
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        run: |
          # This would call Claude Code API if available
          echo "Review the code for security and quality issues" | claude-code
```

### Git Hooks Integration

Create `.git/hooks/post-commit`:

```bash
#!/bin/bash
# Quick review after each commit

echo "Running quick code review on committed files..."
git diff-tree --no-commit-id --name-only -r HEAD | \
  xargs -I {} echo "Review {} for issues" | claude-code --quick
```

---

## 8. Metrics & Reporting

### Track Review Metrics

```bash
# Generate metrics report
"Generate a code quality metrics report for the last 30 days"

# Trend analysis
"Show code quality trends over the last quarter"

# Security posture
"What's our current security posture score?"
```

### Review History

Claude can track:
- Issues found/fixed over time
- Coverage trends
- Security vulnerability trends
- Technical debt accumulation
- Mean time to resolution by severity

---

## 9. Troubleshooting

### Reviews Taking Too Long

**Solution 1: Reduce scope**
```
Review only files changed in the last commit
Review just the platform-security module
```

**Solution 2: Lower severity threshold**
```
Review code but only report P0 and P1 issues
```

### Too Many False Positives

**Solution: Increase confidence threshold**
```
Review code with high confidence threshold (fewer false positives)
Only create issues you're 90% confident about
```

### Missing Issues

**Solution: Increase depth**
```
Run comprehensive deep scan including transitive dependencies
Analyze dead code and unused methods
```

---

## 10. Advanced Usage

### Custom Review Dimensions

```
Review code for:
- Thread safety issues
- Memory leaks
- Performance bottlenecks
- Accessibility compliance
```

### Specific Code Patterns

```
Find all usages of deprecated APIs
Identify all System.out.println() calls
Find hardcoded credentials or API keys
```

### Architectural Reviews

```
Review for architectural violations:
- Layering violations
- Circular dependencies
- Missing abstractions
```

---

## 11. FAQ

**Q: Will auto-fix break my code?**
A: Auto-fix only applies to P0 critical issues with high confidence. Always review changes before merging.

**Q: How does Claude pick which issue to resolve?**
A: Prioritizes by severity (security > bugs > enhancements), feasibility, and impact.

**Q: Can I prevent certain issues from being auto-fixed?**
A: Yes, use labels like `no-auto-fix` or configure exclusions in `.claude/resolve-config.yaml`

**Q: How long does a code review take?**
A: Quick scan (changed files only): 2-5 minutes. Full review: 10-30 minutes depending on codebase size.

**Q: Will this replace human code review?**
A: No, this complements human review. Use for automated checks, humans for architecture/UX decisions.

**Q: Can I run this on private repositories?**
A: Yes, as long as Claude Code has GitHub authentication configured.

---

## 12. Quick Start Checklist

- [ ] Review code manually first: `"Review the code for security issues"`
- [ ] Check results and tune severity threshold
- [ ] Resolve one issue manually: `"Resolve GitHub issue #X"`
- [ ] Verify fix quality and test coverage
- [ ] Set up daily review: `"Configure daily code review at 9am"`
- [ ] Create `.claude/review-config.yaml` with your preferences
- [ ] Enable continuous resolution: `"Keep resolving security issues"`
- [ ] Monitor metrics weekly
- [ ] Iterate on configuration based on results

---

## 13. Support & Resources

**Documentation:**
- `.claude/CONTINUOUS_REVIEW_GUIDE.md` - Detailed review setup
- `.claude/SKILLS_README.md` - Skill reference documentation

**Example Configurations:**
- `.claude/review-config.yaml` - Review settings
- `.claude/resolve-config.yaml` - Issue resolution settings

**GitHub Issues:**
Report problems or request features at your project's issue tracker.

---

**Document Version**: 1.0  
**Last Updated**: May 29, 2026  
**Applicable To**: Claude Code (Sonnet 4.5+)  
**Repository**: FlossWare Platform-Java

---

## Summary

**Instead of typing `/auto-review` or `/auto-resolve`, simply ask Claude in plain English:**

- "Review the code for security issues" → Claude reviews
- "Resolve a GitHub issue" → Claude picks and fixes one
- "Set up daily code reviews" → Claude configures automation

**The capabilities exist - they just don't require slash commands!**
