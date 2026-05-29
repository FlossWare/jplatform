# Custom Skills Setup

## Skills Installed

Four custom productivity skills are installed:

### 1. `/auto-review` - Code Quality Review
One-shot comprehensive code review covering:
- Security vulnerabilities (OWASP Top 10)
- Code quality issues
- Test coverage gaps
- Documentation completeness
- Dependency vulnerabilities

### 2. `/auto-review-loop` - Continuous Code Review
Set up scheduled code reviews (daily/weekly) that automatically create GitHub issues for findings.

### 3. `/auto-resolve` - Issue Resolution
Automatically pick and resolve a GitHub issue from your repository.

### 4. `/auto-resolve-loop` - Continuous Issue Resolution
Continuously resolve GitHub issues until interrupted (autonomous mode).

## Installation Locations

### ✅ Global (Available in ALL Projects)
```
~/.claude/skills/
├── auto-review.md
├── auto-review-loop.md
├── auto-resolve.md
└── auto-resolve-loop.md
```

### ✅ Project-Specific (This Project)
```
.claude/skills/
├── auto-review.md
├── auto-review-loop.md
├── auto-resolve.md
└── auto-resolve-loop.md
```

## Important: Session Restart Required

**Skills are loaded at session startup**, not dynamically. After adding new skills:

1. **Exit current Claude session:**
   ```bash
   # Press Ctrl+D or type:
   exit
   ```

2. **Start new session:**
   ```bash
   claude
   ```

3. **Verify skills loaded:**
   ```bash
   /skills
   ```

You should see the 4 auto-* skills in the list.

## Usage Examples

### Quick Code Review
```bash
/auto-review
```

### Set Up Continuous Reviews
```bash
/auto-review-loop
# Follow prompts to configure schedule (daily/weekly)
```

### Resolve One Issue
```bash
/auto-resolve
# Claude will pick an issue and work on it
```

### Autonomous Issue Resolution
```bash
/auto-resolve-loop
# Claude will continuously resolve issues until interrupted
```

## Troubleshooting

### Skills Not Showing in `/skills` List

**Cause:** Current session started before skills were added.

**Solution:** Exit and restart Claude Code session.

### "Unknown skill" Error

**Cause:** Same as above - session hasn't loaded the skills.

**Solution:** Restart session.

### Skills Work But Not in Global Location

**Cause:** Global skills (`~/.claude/skills/`) may require specific Claude Code version.

**Solution:** Keep project-specific copies as backup. Both locations are currently populated.

## Next Steps

1. **Exit this session** (Ctrl+D)
2. **Restart:** `claude`
3. **Test:** `/auto-review` or `/auto-resolve`
4. **Enjoy!** 🚀

---

**Last Updated:** May 29, 2026  
**Location:** `.claude/SKILLS_README.md`
