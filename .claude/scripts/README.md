# Automated Code Review System

This directory contains scripts for automated code review, issue creation, and fixes.

## Components

### 1. `code_review.sh`
Runs all code quality checks:
- Checkstyle (code style)
- PMD (code quality)
- SpotBugs (bug detection)
- OWASP Dependency Check (security vulnerabilities)
- TODO/FIXME scanner
- Security pattern detection

Outputs are saved to `.claude/review-output/`

### 2. `create_github_issues.py`
Parses review outputs and creates GitHub issues for findings.
- Automatically labels issues with `automated-review`
- Prevents duplicate issues
- Uses GitHub CLI (`gh`)

### 3. `auto_fix_and_push.sh`
Commits and pushes fixes to GitHub main branch.
- Auto-stages all changes
- Creates descriptive commit message
- Includes co-author attribution to Claude

### 4. `review_loop.sh` (Main Entry Point)
Orchestrates the full review cycle:
1. Run code review
2. Create GitHub issues for findings
3. Wait 10 minutes
4. Repeat until no issues found for 2 consecutive iterations

## Usage

### Run full automated review loop:
```bash
./.claude/scripts/review_loop.sh
```

### Run individual components:

```bash
# Just run the review
./.claude/scripts/code_review.sh

# Create issues from existing review outputs
python3 ./.claude/scripts/create_github_issues.py

# Commit and push current changes
./.claude/scripts/auto_fix_and_push.sh
```

## Configuration

The system uses Maven plugins configured in `pom.xml`:
- `maven-checkstyle-plugin`
- `maven-pmd-plugin`
- `spotbugs-maven-plugin`
- `dependency-check-maven-plugin`

Settings for auto-accept are in `.claude/settings.json`.

## GitHub Integration

Requires GitHub CLI (`gh`) to be installed and authenticated:
```bash
gh auth login
```

Issues are created in the `FlossWare/platform-java` repository.

## Stop Condition

The review loop stops when:
- No issues are found in 2 consecutive iterations
- User interrupts with Ctrl+C

## Output

Review outputs are saved to `.claude/review-output/`:
- `checkstyle.txt`
- `pmd.txt`
- `spotbugs.txt`
- `dependency-check.txt`
- `todos.txt`
- `security-patterns.txt`
