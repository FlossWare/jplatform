# Automated Code Review System

## Overview

Fully automated code review system that runs every 10 minutes, creates GitHub issues for findings, and stops when clean.

## Language-Specific Checks

### Python Files
- ✓ **mypy** - Type checking
- ✓ **flake8** - Style and quality
- ✓ **bandit** - Security scanner
- ✓ **Security scans** - Risky pattern detection
- ✓ **TODO checks** - TODO/FIXME/XXX/HACK

### Java Files
- ✓ **Security scans** - Risky pattern detection only
- ✓ **TODO checks** - TODO/FIXME/XXX/HACK only

## Components Created

### 1. Auto-Accept Permissions ✓
**File**: `.claude/settings.json`

All tools and operations auto-accepted:
- Bash, Write, Edit, Read
- TaskCreate, TaskUpdate, TaskList, TaskGet
- All git commands
- All Maven commands
- GitHub CLI commands
- Script execution

**No permission prompts for anything!**

### 2. Review Scripts

#### `.claude/scripts/code_review.sh`
Main review script that detects file types and runs appropriate checks.

**For Python:**
1. mypy (type checking)
2. flake8 (style/quality)
3. bandit (security)
4. Security pattern scan
5. TODO checks

**For Java:**
1. Security pattern scan (System.out, printStackTrace, Runtime, ProcessBuilder, etc.)
2. TODO checks

Outputs saved to: `.claude/review-output/`

#### `.claude/scripts/create_review_issues.py`
- Parses all review outputs (Python and Java)
- Creates GitHub issues for findings
- Prevents duplicates
- Labels with `automated-review`
- Security findings also get `security` label

#### `.claude/scripts/auto_fix_and_push.sh`
- Commits all changes
- Pushes to main branch
- Co-authored by Claude Sonnet 4.5

#### `.claude/scripts/review_loop.sh` (Main Entry Point)
Orchestrates the full cycle:
```
Review → Create Issues → Wait 10 min → Repeat
```
Stops when no new issues found.

## How to Start

### Start the Review Loop
```bash
./.claude/scripts/review_loop.sh
```

This runs automatically every 10 minutes until the code is clean.

## What Gets Checked

### Python Security Patterns
- `eval`
- `exec`
- `__import__`
- `pickle.loads`
- `yaml.load` (without safe_load)
- `subprocess.call`
- `os.system`

### Java Security Patterns
- `System.out.println`
- `printStackTrace`
- `Runtime.getRuntime`
- `ProcessBuilder`
- `Class.forName`
- `exec`
- `eval`

### TODO Patterns (All Languages)
- TODO
- FIXME
- XXX
- HACK

## Issue Creation

All issues created in: `FlossWare/platform-java`

### Issue Types

**Python:**
- `[Auto Review] MyPy found N type errors`
- `[Auto Review] Flake8 found N style/quality issues`
- `[Auto Review] Bandit found security vulnerabilities in Python`
- `[Auto Review] N risky Python code patterns detected`

**Java:**
- `[Auto Review] N risky Java code patterns detected`

**Common:**
- `[Auto Review] N TODO/FIXME comments found`

### Issue Labels
- `automated-review` - All automated issues
- `security` - Security findings (added to security-related issues)

### Deduplication
- Checks existing open issues
- Skips if same title exists
- Prevents spam

## Stop Condition

The loop stops when:
1. **No new issues found** (no threshold, just clean run)
2. **User presses Ctrl+C**

## Review Outputs

Location: `.claude/review-output/`

**Python files:**
- `mypy.txt` - Type errors
- `flake8.txt` - Style/quality issues
- `bandit.txt` - Security vulnerabilities
- `python-security-scans.txt` - Risky patterns

**Java files:**
- `java-security-scans.txt` - Risky patterns

**Common:**
- `todo-checks.txt` - TODO comments

## Integration

### GitHub
- Uses `gh` CLI (must be authenticated)
- Creates issues automatically
- Repository: `FlossWare/platform-java`

### Git
- Auto-commits with descriptive messages
- Pushes to `main` branch
- Includes Claude co-author attribution

## Workflow

```
┌──────────────────┐
│  Review Loop     │
│  (Every 10 min)  │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│ code_review.sh   │ ← Python: 5 checks
│                  │   Java: 2 checks
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│ create_review_   │ ← Parses outputs
│ issues.py        │   Creates GitHub issues
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│ Claude reads     │ ← Fixes the issues
│ issues and fixes │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│ auto_fix_and_    │ ← Commits & pushes
│ push.sh          │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│ Wait 10 minutes  │
└────────┬─────────┘
         │
         └─────────► Repeat until clean
```

## Configuration

### Change Review Interval
Edit `review_loop.sh`:
```bash
sleep 600  # Change 600 to desired seconds
```

### Customize Issue Creation
Edit `create_review_issues.py`:
- `ISSUE_LABEL` - Change label
- `ISSUE_PREFIX` - Change title prefix
- Parser functions - Customize issue detection

## Prerequisites

- ✓ Python 3.6+ with mypy, flake8, bandit installed
- ✓ GitHub CLI authenticated (`gh auth login`)
- ✓ Git configured (user.name, user.email)
- ✓ Write access to `FlossWare/platform-java`

## Installing Python Tools

If mypy, flake8, or bandit are missing:
```bash
pip install mypy flake8 bandit
```

## Next Steps

1. **Start the review loop**:
   ```bash
   ./.claude/scripts/review_loop.sh
   ```

2. **Monitor issues created**:
   ```bash
   gh issue list --label automated-review
   ```

3. **Fix issues** as they are created

4. **Watch progress**:
   ```bash
   # View review outputs
   ls -lh .claude/review-output/
   
   # View commit log
   git log --oneline -10
   ```

## Stopping the Loop

Press `Ctrl+C` to stop the review loop at any time.

## Success Criteria

Loop completes successfully when:
- All checks pass
- No issues found
- Clean run

Message displayed:
```
╔════════════════════════════════════════╗
║   Review Loop Complete - No Issues!    ║
╚════════════════════════════════════════╝
```

## File Organization

```
.claude/
├── settings.json              # Auto-accept permissions
├── scripts/
│   ├── code_review.sh         # Main review script
│   ├── create_review_issues.py # Issue creation
│   ├── auto_fix_and_push.sh   # Commit & push
│   ├── review_loop.sh         # Main loop (entry point)
│   └── README.md              # Script docs
├── review-output/             # Created on first run
│   ├── mypy.txt               # Python type errors
│   ├── flake8.txt             # Python style issues
│   ├── bandit.txt             # Python security
│   ├── python-security-scans.txt # Python patterns
│   ├── java-security-scans.txt   # Java patterns
│   └── todo-checks.txt        # TODO comments
├── REVIEW_SYSTEM.md           # This file
└── QUICKSTART_REVIEW.md       # Quick start guide
```
