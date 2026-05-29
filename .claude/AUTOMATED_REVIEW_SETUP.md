# Automated Code Review System - Setup Complete

## What Was Created

### 1. Auto-Accept Permissions
**File**: `.claude/settings.json`

All permissions for the following are auto-accepted:
- `Bash` tool (for git, Maven, scripts)
- `Write`, `Edit`, `Read` tools
- `TaskCreate`, `TaskUpdate`, `TaskList`, `TaskGet`
- Patterns for common operations: git commands, Maven, gh CLI, script execution

### 2. Code Review Scripts
**Directory**: `.claude/scripts/`

#### `code_review.sh`
Runs 6 different code quality checks:
1. **Checkstyle** - Code style violations
2. **PMD** - Code quality issues
3. **SpotBugs** - Potential bugs
4. **OWASP Dependency Check** - Security vulnerabilities
5. **TODO/FIXME Scanner** - Tracks technical debt
6. **Security Pattern Scan** - Finds risky patterns (System.out, printStackTrace, etc.)

#### `create_github_issues.py`
- Parses review outputs
- Creates GitHub issues automatically
- Prevents duplicates
- Labels with `automated-review`
- Adds security labels for security findings

#### `auto_fix_and_push.sh`
- Stages all changes
- Creates commit with descriptive message
- Pushes to main branch
- Includes Claude co-author attribution

#### `review_loop.sh` (Main Script)
- Runs review → create issues → wait 10 minutes → repeat
- Stops after 2 consecutive clean iterations
- Fully automated

### 3. Documentation
- `README.md` in scripts directory
- This setup guide

## How to Use

### Start the Automated Review Loop
```bash
cd /home/sfloess/Development/github/FlossWare/platform-java
./.claude/scripts/review_loop.sh
```

This will:
1. Run all 6 code quality checks
2. Parse outputs and create GitHub issues
3. Wait 10 minutes
4. Repeat until clean for 2 iterations

### Manual Operations

Run just the review:
```bash
./.claude/scripts/code_review.sh
```

Create issues from existing review:
```bash
python3 ./.claude/scripts/create_github_issues.py
```

Commit and push current changes:
```bash
./.claude/scripts/auto_fix_and_push.sh
```

## Integration Points

### GitHub
- Uses `gh` CLI for issue creation
- Creates issues in `FlossWare/platform-java`
- All issues labeled `automated-review`
- Security issues also get `security` label

### Maven
Uses these plugins (already configured in `pom.xml`):
- `maven-checkstyle-plugin`
- `maven-pmd-plugin`
- `spotbugs-maven-plugin`
- `dependency-check-maven-plugin`

### Git
- Auto-commits with descriptive messages
- Pushes to `main` branch
- Co-authored by Claude Sonnet 4.5

## Review Output Location

All review outputs saved to: `.claude/review-output/`

Files created:
- `checkstyle.txt`
- `pmd.txt`
- `spotbugs.txt`
- `dependency-check.txt`
- `todos.txt`
- `security-patterns.txt`

## Stop Conditions

The loop stops when:
1. **Success**: No issues found for 2 consecutive iterations
2. **Manual**: User presses Ctrl+C

## Next Steps

### To Start the Review Process:
```bash
# Start the automated review loop
./.claude/scripts/review_loop.sh
```

### To Fix Issues Created:
Claude will:
1. Read the GitHub issues created by the script
2. Fix the issues in the code
3. Use `auto_fix_and_push.sh` to commit and push
4. Loop continues until clean

### To Monitor Progress:
```bash
# Check review outputs
ls -lh .claude/review-output/

# Check GitHub issues
gh issue list --label automated-review

# Watch git log
git log --oneline -10
```

## Configuration Notes

- All scripts are executable
- Python script requires Python 3.6+
- GitHub CLI must be authenticated (`gh auth login`)
- Maven must be available on PATH
- Git must be configured with user name/email

## Workflow Summary

```
┌─────────────────┐
│  Review Loop    │
│   (Every 10m)   │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  code_review.sh │ ← Runs 6 checks
└────────┬────────┘
         │
         ▼
┌─────────────────────┐
│ create_github_      │ ← Parses outputs
│ issues.py           │   Creates issues
└────────┬────────────┘
         │
         ▼
┌─────────────────────┐
│ Claude reads issues │
│ and fixes code      │
└────────┬────────────┘
         │
         ▼
┌─────────────────────┐
│ auto_fix_and_push.sh│ ← Commits & pushes
└────────┬────────────┘
         │
         ▼
┌─────────────────────┐
│ Wait 10 minutes     │
└────────┬────────────┘
         │
         └──────► (Repeat until clean)
```

## Customization

### Change Review Interval
Edit `review_loop.sh`, line with `sleep 600` (600 seconds = 10 minutes)

### Change Stop Condition
Edit `review_loop.sh`, line with `if [ $NO_ISSUES_COUNT -ge 2 ]` (currently 2 clean iterations)

### Add More Checks
Edit `code_review.sh` to add more Maven goals or custom checks

### Modify Issue Labels
Edit `create_github_issues.py`, variables `ISSUE_LABEL` and `ISSUE_PREFIX`

## Troubleshooting

### GitHub CLI Not Authenticated
```bash
gh auth login
```

### Maven Commands Failing
```bash
# Test Maven is working
mvn --version

# Run a clean build first
mvn clean install
```

### Permission Issues
All permissions are auto-accepted in `.claude/settings.json`

### Review Outputs Empty
Check that Maven plugins are configured in `pom.xml`
