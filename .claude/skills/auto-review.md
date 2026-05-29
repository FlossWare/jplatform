---
name: auto-review
description: Run automated code review, create GitHub issues, and optionally start the review loop
---

# Auto Review Skill

Run the automated code review system on the current project.

## What This Does

1. **Detects project type** - Identifies if it's Java, Python, Shell, or mixed
2. **Runs code quality checks** - Language-appropriate checks
3. **Creates GitHub issues** - Auto-creates issues for any findings
4. **Optional loop mode** - Can run continuously every 10 minutes

## Usage

```
/auto-review [options]
```

### Options

- No args: Run review once and create issues
- `loop`: Start the continuous review loop (runs every 10 minutes)
- `--help`: Show this help

## Examples

```
/auto-review
```
Run review once on current project.

```
/auto-review loop
```
Start continuous review loop (every 10 minutes until clean).

## Instructions

When the user invokes `/auto-review`:

1. **Check if .claude/scripts exists**
   - If not, tell user the project isn't configured
   - Show them how to run the installer: `/home/sfloess/Development/github/FlossWare/apply-review-system.sh`

2. **Run the appropriate command**
   - For single run: `./.claude/scripts/code_review.sh` then `python3 ./.claude/scripts/create_review_issues.py`
   - For loop mode: `./.claude/scripts/review_loop.sh` in background

3. **Show results**
   - Display the review summary
   - Show how many issues were created (if any)
   - Provide GitHub issue links

4. **If loop mode**
   - Start the loop in background
   - Tell user how to check progress
   - Explain the stop condition (2 consecutive clean runs)

## Code Quality Checks by Language

### Python
- mypy (type checking)
- flake8 (style/quality)
- bandit (security)
- Security patterns
- TODO checks

### Java
- Security scans
- TODO checks

### Shell Scripts
- shellcheck (if available)
- Security patterns
- TODO checks

## After Review

If issues are found:
1. GitHub issues are auto-created
2. Show issue URLs
3. Ask if user wants Claude to fix them
4. If yes, read the issues and fix the code
5. Auto-commit and push with `./.claude/scripts/auto_fix_and_push.sh`

## Stop Condition (Loop Mode)

The loop stops when:
- No issues found for 2 consecutive iterations
- User presses Ctrl+C (or stops the background task)

## Background Task

When running in loop mode, use `run_in_background: true` for the Bash tool so the user can continue working while the review runs.

Notify the user they can:
- Check progress: `tail -f .claude/review-output/*.txt`
- View issues: `gh issue list --label automated-review`
- Stop loop: Press Ctrl+C in the terminal running the loop

## Success Message

When complete, show:
```
✓ Code Review Complete!

Results:
- Python checks: X/Y passing
- Java checks: X/Y passing
- Issues created: N

GitHub Issues: [URLs]

All systems operational!
```

## Error Handling

If `.claude/scripts/` doesn't exist:
```
⚠️  Auto-review not configured for this project.

To set up auto-review:
1. Run: /home/sfloess/Development/github/FlossWare/apply-review-system.sh
2. Or manually copy scripts from another configured project

Configured projects: platform-java, VirtOS, and 26 others
```

## Related Commands

- View review results: `cat .claude/review-output/*.txt`
- List issues: `gh issue list --label automated-review`
- Manual review: `./.claude/scripts/code_review.sh`
- Start loop: `./.claude/scripts/review_loop.sh`

---

**Note:** This skill uses the automated code review system installed in `.claude/scripts/`. All permissions are auto-accepted (zero prompts).
