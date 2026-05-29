#!/usr/bin/env python3
"""
Create GitHub issues from code review findings.
Uses GitHub CLI (gh) to create issues.
"""

import os
import sys
import json
import subprocess
import hashlib
from pathlib import Path
from datetime import datetime

REVIEW_OUTPUT_DIR = Path(__file__).parent.parent / "review-output"
REPO = "FlossWare/platform-java"
ISSUE_LABEL = "automated-review"
ISSUE_PREFIX = "[Auto Review]"


def run_gh_command(cmd):
    """Run a gh CLI command and return the output."""
    try:
        result = subprocess.run(
            cmd,
            shell=True,
            capture_output=True,
            text=True,
            check=True
        )
        return result.stdout.strip()
    except subprocess.CalledProcessError as e:
        print(f"Error running command: {cmd}", file=sys.stderr)
        print(f"Error: {e.stderr}", file=sys.stderr)
        return None


def get_existing_issues():
    """Get all existing automated review issues."""
    cmd = f'gh issue list --repo {REPO} --label "{ISSUE_LABEL}" --json number,title,state --limit 1000'
    output = run_gh_command(cmd)
    if output:
        return json.loads(output)
    return []


def create_issue_hash(title, body):
    """Create a hash of the issue content for deduplication."""
    content = f"{title}\n{body}"
    return hashlib.md5(content.encode()).hexdigest()[:8]


def parse_checkstyle_output(file_path):
    """Parse checkstyle output and extract issues."""
    if not file_path.exists() or file_path.stat().st_size == 0:
        return []

    issues = []
    with open(file_path) as f:
        content = f.read()
        if "[ERROR]" in content or "violations" in content.lower():
            issues.append({
                "title": f"{ISSUE_PREFIX} Checkstyle violations found",
                "body": f"Checkstyle found code style violations.\n\n```\n{content[:2000]}\n```\n\nSee `.claude/review-output/checkstyle.txt` for full details.",
                "type": "checkstyle"
            })
    return issues


def parse_pmd_output(file_path):
    """Parse PMD output and extract issues."""
    if not file_path.exists() or file_path.stat().st_size == 0:
        return []

    issues = []
    with open(file_path) as f:
        content = f.read()
        if "[ERROR]" in content or "violations" in content.lower():
            issues.append({
                "title": f"{ISSUE_PREFIX} PMD violations found",
                "body": f"PMD found code quality issues.\n\n```\n{content[:2000]}\n```\n\nSee `.claude/review-output/pmd.txt` for full details.",
                "type": "pmd"
            })
    return issues


def parse_spotbugs_output(file_path):
    """Parse SpotBugs output and extract issues."""
    if not file_path.exists() or file_path.stat().st_size == 0:
        return []

    issues = []
    with open(file_path) as f:
        content = f.read()
        if "bugs" in content.lower() and "total" in content.lower():
            issues.append({
                "title": f"{ISSUE_PREFIX} SpotBugs found potential bugs",
                "body": f"SpotBugs detected potential bugs in the code.\n\n```\n{content[:2000]}\n```\n\nSee `.claude/review-output/spotbugs.txt` for full details.",
                "type": "spotbugs"
            })
    return issues


def parse_dependency_check_output(file_path):
    """Parse OWASP dependency check output and extract issues."""
    if not file_path.exists() or file_path.stat().st_size == 0:
        return []

    issues = []
    with open(file_path) as f:
        content = f.read()
        if "vulnerabilit" in content.lower():
            issues.append({
                "title": f"{ISSUE_PREFIX} Security vulnerabilities in dependencies",
                "body": f"OWASP Dependency Check found security vulnerabilities.\n\n```\n{content[:2000]}\n```\n\nSee `.claude/review-output/dependency-check.txt` for full details.",
                "type": "security",
                "labels": ["security", ISSUE_LABEL]
            })
    return issues


def parse_todos(file_path):
    """Parse TODO/FIXME comments."""
    if not file_path.exists() or file_path.stat().st_size == 0:
        return []

    with open(file_path) as f:
        lines = f.readlines()

    if len(lines) > 20:
        issues = [{
            "title": f"{ISSUE_PREFIX} {len(lines)} TODO/FIXME comments found",
            "body": f"Found {len(lines)} TODO/FIXME comments in the codebase.\n\n```\n{''.join(lines[:20])}\n... and {len(lines) - 20} more\n```\n\nSee `.claude/review-output/todos.txt` for full list.",
            "type": "todos"
        }]
        return issues
    return []


def parse_security_patterns(file_path):
    """Parse security pattern scan results."""
    if not file_path.exists() or file_path.stat().st_size == 0:
        return []

    issues = []
    with open(file_path) as f:
        content = f.read()
        lines = [l for l in content.split('\n') if l.strip() and not l.startswith('===')]

        if lines:
            issues.append({
                "title": f"{ISSUE_PREFIX} {len(lines)} potential security patterns detected",
                "body": f"Found {len(lines)} instances of potentially risky patterns.\n\n```\n{content[:2000]}\n```\n\nSee `.claude/review-output/security-patterns.txt` for full details.",
                "type": "security-patterns",
                "labels": ["security", ISSUE_LABEL]
            })
    return issues


def create_github_issue(issue):
    """Create a GitHub issue."""
    title = issue["title"]
    body = issue["body"]
    labels = issue.get("labels", [ISSUE_LABEL])

    # Add metadata
    body += f"\n\n---\n*Generated by automated code review on {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}*"

    # Create issue
    labels_str = ",".join(labels)
    cmd = f'gh issue create --repo {REPO} --title "{title}" --body "{body}" --label "{labels_str}"'

    result = run_gh_command(cmd)
    if result:
        print(f"✓ Created issue: {title}")
        return True
    else:
        print(f"✗ Failed to create issue: {title}")
        return False


def main():
    """Main function to process review outputs and create issues."""
    if not REVIEW_OUTPUT_DIR.exists():
        print(f"Review output directory not found: {REVIEW_OUTPUT_DIR}")
        return

    print("Parsing review outputs...")
    all_issues = []

    # Parse each type of output
    parsers = [
        ("checkstyle.txt", parse_checkstyle_output),
        ("pmd.txt", parse_pmd_output),
        ("spotbugs.txt", parse_spotbugs_output),
        ("dependency-check.txt", parse_dependency_check_output),
        ("todos.txt", parse_todos),
        ("security-patterns.txt", parse_security_patterns),
    ]

    for filename, parser in parsers:
        file_path = REVIEW_OUTPUT_DIR / filename
        issues = parser(file_path)
        all_issues.extend(issues)

    if not all_issues:
        print("No issues found in review outputs.")
        return

    print(f"Found {len(all_issues)} potential issues")

    # Get existing issues to avoid duplicates
    existing_issues = get_existing_issues()
    existing_titles = {issue["title"] for issue in existing_issues if issue["state"] == "open"}

    # Create new issues
    created_count = 0
    for issue in all_issues:
        if issue["title"] in existing_titles:
            print(f"⊘ Skipping duplicate: {issue['title']}")
            continue

        if create_github_issue(issue):
            created_count += 1

    print(f"\nCreated {created_count} new issues out of {len(all_issues)} findings")


if __name__ == "__main__":
    main()
