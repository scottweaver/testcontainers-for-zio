---
# Project skill bindings — consumed by ~/.claude/skills/daily-stand-up and ~/.claude/skills/wrap-up.
# Hand-edited rule file; the portable skills are project-agnostic, all the per-project values live here.
# Schema source of truth: ~/.claude/skills/bootstrap-project/SKILL.md

tracker: github

# Linear bindings (required when tracker: linear; ignored otherwise).
# linear:
#   workspace: <slug>
#   team_prefix: <PREFIX>
#   assignee_id: <uuid>
#   state_uuids:
#     in_progress: <uuid>
#     in_review:   <uuid>
#     done:        <uuid>

# GitHub bindings (required when tracker: github; ignored otherwise).
github:
  owner: scottweaver
  repo:  testcontainers-for-zio
  assignee: scottweaver

# Project state file consumed by daily-stand-up (T discovery) and wrap-up (step-4 refresh).
# Set state_file.path to null to disable both behaviours.
state_file:
  path: .claude/rules/STATE.md
  next_up_patterns:
    - "**Next: {ID}**"
    - "**next up: {ID}**"
    - "Next up in Phase N: {ID}"

# Stand-up-only settings.
# Personal OSS project with no stand-up; block left commented out so the
# daily-stand-up skill is inert here. Uncomment if that ever changes.
# standup:
#   no_blockers_sentinel: ":none:"
#   # email_to: <address>           # uncomment to have daily-stand-up offer email delivery

# Wrap-up-only settings.
wrapup:
  docs_pr_branch_prefix: docs/state-post-
  docs_pr_commit_prefix: "docs(state):"
  auto_merge_carve_out_path: .claude/rules/
  state_refresh_authority: null
  audit_doc:
    path: .claude/rules/ARCHITECTURE.md
    trigger_checklist_section: Audit triggers
    structural_criteria_section: Structural criteria
---

# Project bindings — testcontainers-for-zio

Personal open-source Scala library (ZIO wrappers around Testcontainers). Hosted on
GitHub at `scottweaver/testcontainers-for-zio`; issues and PRs live there.

- **No stand-up.** This is a solo project with no daily status ritual, so the
  `standup` block is commented out. `daily-stand-up` should not be run here.
- **State file.** `.claude/rules/STATE.md` tracks current focus and the next-up
  item. `wrap-up` refreshes it after each merged PR via the docs carve-out.
- **Branching.** Default branch is `main`. Docs-only PRs touching only
  `.claude/rules/` are auto-merge-eligible per the `wrapup` carve-out.
- **Audit doc.** `.claude/rules/ARCHITECTURE.md` lists audit triggers and
  structural criteria; `wrap-up` checks each merged PR's files against them.
