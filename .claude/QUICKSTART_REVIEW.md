# Automated Code Review - Quick Start

## ✅ Setup Complete!

All components are installed and configured.

## Start the Review System

```bash
cd /home/sfloess/Development/github/FlossWare/platform-java
./.claude/scripts/review_loop.sh
```

That's it! The system will:
1. ✓ Run code review (language-specific checks)
2. ✓ Create GitHub issues for any findings
3. ✓ Wait 10 minutes
4. ✓ Repeat until no new issues

## What Gets Checked

### Python Files (`.py`)
- ✓ **mypy** - Type checking
- ✓ **flake8** - Style and quality
- ✓ **bandit** - Security vulnerabilities
- ✓ **Security scans** - Risky patterns (eval, exec, pickle, etc.)
- ✓ **TODO checks** - TODO/FIXME/XXX/HACK

### Java Files (`.java`)
- ✓ **Security scans** - Risky patterns (System.out, printStackTrace, Runtime, etc.)
- ✓ **TODO checks** - TODO/FIXME/XXX/HACK

## What Happens

### First Run
```
========================================
Starting Code Review - 2026-05-29 09:40
========================================

Found 3 Python files, 142 Java files

=== Python Code Checks ===
[1/3] Running mypy...
[2/3] Running flake8...
[3/3] Running bandit...

=== Java Code Checks ===
[1/1] Running security scans...

=== TODO/FIXME Checks ===
Found 87 TODO/FIXME comments

Creating remote issues...
✓ Created issue: [Auto Review] Flake8 found 5 style/quality issues
✓ Created issue: [Auto Review] 12 risky Java code patterns detected
✓ Created issue: [Auto Review] 87 TODO/FIXME comments found

Waiting 10 minutes before next iteration...
```

### After Claude Fixes Issues
```
Iteration #2
...
✓ No issues found (count: 1/2)

Iteration #3
...
✓ No issues found (count: 2/2)

╔════════════════════════════════════════╗
║   Review Loop Complete - No Issues!    ║
╚════════════════════════════════════════╝
```

## Your Role

When issues are created:
1. Claude will read the GitHub issues
2. Claude will fix the code
3. Claude will commit and push automatically
4. Loop continues until all issues resolved

## Monitoring

### View Issues Created
```bash
gh issue list --label automated-review
```

### View Review Outputs
```bash
ls -lh .claude/review-output/
cat .claude/review-output/mypy.txt
cat .claude/review-output/java-security-scans.txt
```

### Watch Commits
```bash
git log --oneline -10
```

## Manual Control

### Run Review Only (no loop)
```bash
./.claude/scripts/code_review.sh
```

### Create Issues Manually
```bash
python3 ./.claude/scripts/create_review_issues.py
```

### Commit & Push Manually
```bash
./.claude/scripts/auto_fix_and_push.sh
```

## Stop the Loop

Press `Ctrl+C` at any time.

## All Settings Auto-Accepted

No permission prompts for:
- ✓ Git commands (status, diff, add, commit, push)
- ✓ Maven commands (all mvn operations)
- ✓ GitHub CLI (issue creation)
- ✓ File operations (read, write, edit)
- ✓ Script execution
- ✓ Python tools (mypy, flake8, bandit)

Everything runs automatically!

## Prerequisites Check

Before starting, verify Python tools are installed:

```bash
# Check if tools are available
mypy --version
flake8 --version
bandit --version

# If missing, install:
pip install mypy flake8 bandit
```

## Configuration Files

- `.claude/settings.json` - Auto-accept permissions
- `.claude/scripts/` - All review scripts
- `.claude/review-output/` - Review results (created on first run)
- `.claude/REVIEW_SYSTEM.md` - Full documentation

## Review Outputs

Location: `.claude/review-output/`

**Python:**
- `mypy.txt`
- `flake8.txt`
- `bandit.txt`
- `python-security-scans.txt`

**Java:**
- `java-security-scans.txt`

**Common:**
- `todo-checks.txt`

## Ready to Go!

Just run:
```bash
./.claude/scripts/review_loop.sh
```

The automated review begins immediately!

## What Makes This Special

### Language-Aware
- Detects Python vs Java files automatically
- Runs appropriate checks for each language
- Skips irrelevant checks

### Lightweight for Java
- Only security scans and TODO checks
- No heavy static analysis
- Fast execution

### Comprehensive for Python
- Full suite: mypy, flake8, bandit
- Security pattern detection
- Style and quality enforcement

### Fully Automated
- Zero permission prompts
- Auto-creates GitHub issues
- Auto-commits and pushes fixes
- Runs every 10 minutes

### Smart Deduplication
- Won't create duplicate issues
- Tracks what's already reported
- Only creates new findings

## Example Session

```bash
$ ./.claude/scripts/review_loop.sh

╔════════════════════════════════════════╗
║  Automated Code Review Loop Starting   ║
╚════════════════════════════════════════╝

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  Iteration #1
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Step 1: Running code review...
Found 3 Python files, 142 Java files

=== Python Code Checks ===
[1/3] Running mypy...
✓ MyPy: PASSED
[2/3] Running flake8...
✗ Flake8: FOUND ISSUES
[3/3] Running bandit...
✓ Bandit: PASSED

=== Java Code Checks ===
[1/1] Running security scans...
✗ Found 12 security patterns in Java code

=== TODO/FIXME Checks ===
Found 87 TODO/FIXME comments

Step 2: Creating remote issues...
✓ Created issue: [Auto Review] Flake8 found 5 style/quality issues
✓ Created issue: [Auto Review] 12 risky Java code patterns detected
✓ Created issue: [Auto Review] 87 TODO/FIXME comments found

Created 3 new issues out of 3 findings

Waiting 10 minutes before next iteration...
(Press Ctrl+C to stop)
```

Now Claude fixes the issues, commits, and pushes...

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  Iteration #2
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Step 1: Running code review...
Found 3 Python files, 142 Java files

All checks passed!

✓ No issues found (count: 1/2)

Waiting 10 minutes before next iteration...
```

One more clean run...

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  Iteration #3
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Step 1: Running code review...
✓ No issues found (count: 2/2)

╔════════════════════════════════════════╗
║   Review Loop Complete - No Issues!    ║
╚════════════════════════════════════════╝

All code quality checks passed for 2 consecutive iterations.
```

Done!
