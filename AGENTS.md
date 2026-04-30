<!-- br-agent-instructions-v1 -->

---

## Beads Workflow Integration

This project uses [beads_rust](https://github.com/Dicklesworthstone/beads_rust) (`br`/`bd`) for issue tracking. Issues are stored in `.beads/` and tracked in git.

### Essential Commands

```bash
# View ready issues (open, unblocked, not deferred)
br ready              # or: bd ready

# List and search
br list --status=open # All open issues
br show <id>          # Full issue details with dependencies
br search "keyword"   # Full-text search

# Create and update
br create --title="..." --description="..." --type=task --priority=2
br update <id> --status=in_progress
br close <id> --reason="Completed"
br close <id1> <id2>  # Close multiple issues at once

# Sync with git
br sync --flush-only  # Export DB to JSONL
br sync --status      # Check sync status
```

### Workflow Pattern

1. **Start**: Run `br ready` to find actionable work
2. **Claim**: Use `br update <id> --status=in_progress`
3. **Work**: Implement the task
4. **Complete**: Use `br close <id>`
5. **Sync**: Always run `br sync --flush-only` at session end

### Key Concepts

- **Dependencies**: Issues can block other issues. `br ready` shows only open, unblocked work.
- **Priority**: P0=critical, P1=high, P2=medium, P3=low, P4=backlog (use numbers 0-4, not words)
- **Types**: task, bug, feature, epic, chore, docs, question
- **Blocking**: `br dep add <issue> <depends-on>` to add dependencies

### Session Protocol

**Before ending any session, run this checklist:**

```bash
git status              # Check what changed
git add <files>         # Stage code changes
br sync --flush-only    # Export beads changes to JSONL
git commit -m "..."     # Commit everything
git push                # Push to remote
```

### Epic / Subtask Convention

Group related tasks under an epic. Create the epic first, then create subtasks with `--parent <EPIC_ID>`. Beads auto-assigns IDs in the form `EPIC_ID.1`, `EPIC_ID.2`, etc. — do **not** put the epic ID in the title.

```bash
br create --title="My Epic" --type=epic --priority=0
# note the returned ID, e.g. bwa-abc
br create --title="First subtask" --type=task --priority=0 --parent=bwa-abc
# → bwa-abc.1
br create --title="Second subtask" --type=task --priority=0 --parent=bwa-abc
# → bwa-abc.2
```

### Dependencies

Wire blockers with `br dep add <blocked> <blocker>`. Combined with `--parent`, this means `br ready` always shows only truly unblocked work.

```bash
br dep add bwa-abc.2 bwa-abc.1   # abc.2 is blocked until abc.1 is closed
br dep add bwa-abc.2 bwa-xyz.1   # can depend on tasks in other epics too
```

You can also specify deps at creation time with `--deps type:id,type:id`.

### Best Practices

- Check `br ready` at session start to find available work
- Update status as you work (in_progress → closed)
- Create new issues with `br create` when you discover tasks
- Use descriptive titles and set appropriate priority/type
- Always sync before ending session
- Use epics to group work; subtasks get auto-numbered IDs via `--parent`
- Wire all blockers with `br dep add` so `br ready` stays meaningful

<!-- end-br-agent-instructions -->
