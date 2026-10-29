# Methodologies and Guidelines

General guidelines for how work happens in this project — workflow,
branching, PRs, refactors, and post-merge hygiene.

## Buy versus build

If there is an ecosystem library that solves a problem, prefer it over
building from scratch. Web searches are permitted (and encouraged) to
find prior art before writing anything substantial.

Vetting bar before adopting a dependency: recent maintenance activity,
reasonable download/reverse-dependency numbers, no giant transitive
tree for a small feature, and license compatibility. (Rust: `cargo
tree` before `cargo add` when the dependency is non-trivial. JS/TS:
check the lockfile delta.) Dependencies that sit at the core of the
architecture are architecture decisions, not conveniences — those get
a design dialog before adoption.

## Git policies

When starting work on a new feature, do it in a new branch. Prompt
whether to branch from `main` or from the currently checked-out
branch.

Confirm before destructive operations: branch deletion, history
rewrites, force pushes, file deletion outside `/tmp`.

## Strive to add as little code as possible

Before making code changes or proposals, thoroughly evaluate the
existing code and find places where existing logic can be reused.
Refactoring to achieve this is encouraged.

The goal is the smallest **long-run** codebase, not the smallest diff.
Better, more readable, and more reusable code always trumps strict
"fewest lines" thinking — extracting a clear helper today (even at
+10 lines) prevents +50 lines of duplication tomorrow.

What this still rules out:

- Speculative abstractions for hypothetical future requirements.
- Gratuitous wrapping (a one-line helper whose only job is to rename
  a call).
- Over-engineered type/class machinery when a function does the job.

What this encourages:

- Extracting a shared predicate/helper when the same check appears in
  three or more places.
- Renaming or splitting a function whose name no longer describes
  what it does.
- Promoting a pattern to a utility once a third call site appears.

When in doubt, optimise for the next reader, not the smallest patch.

## Push / PR only when the work moves the needle

Branches are pushed and opened as PRs only when the work is
observable progress: a test flips from failing to passing, a feature
gains a working, tested increment, or a refactor explicitly labeled
as such achieves a stated structural improvement.

Foundation work that prepares the way but doesn't itself land
observable progress stays on a local branch until it can be combined
with the work that does, or rolled into a successor branch. This
keeps the remote and review queue focused on progress and avoids
cluttering the project with in-flight scaffolding.

## After a PR merges

When a PR merges to `main`, the work is shipped but not yet
**closed**. A small cleanup routine propagates the merge to the parts
of the project that don't update themselves. Skipping steps creates
drift — stale branches accumulate, tickets read "In Progress" weeks
after the code shipped, docs describe a world that no longer exists.

Run this as soon as the PR is merged. **Confirm with the user before
the destructive steps** if acting unprompted.

1. **Pull `main` and switch off the merged branch.**
   `git checkout main && git pull --ff-only`
2. **Delete the merged branch — local and remote.** Use `git branch -d`
   (not `-D`); if `-d` refuses, the branch isn't actually merged —
   investigate before forcing. (Squash-merge warnings about
   "not yet merged to HEAD" are expected.)
3. **Verify the tracker ticket state** matches reality; flip it
   manually if needed (see LINEAR.md, if this project uses Linear).
4. **Refresh STATE.md** — and audit ARCHITECTURE.md if the merge
   touched anything in its audit-trigger list.
5. **Docs-only PRs under `.claude/rules/` may skip human review** —
   CI gates them like everything else, and the diff is pure
   documentation carrying no runtime risk. Anything touching source,
   tests, workflows, or scripts goes through normal review.

A PR closed *without* merging has its own cleanup: ticket → Cancelled
(or back to Backlog if work will resume), branch deleted if
abandoned, no doc refresh.

## Refactoring

Refactors don't flip tests. They rely entirely on the explanation to
justify themselves — at review time and later, when someone decides
whether to extend the new structure or revert it. The commit message
and the tracker ticket comment are the durable record of *why*.

Every refactor commit + ticket comment must answer four questions:

1. **What changed.** Terse — the diff is authoritative.
2. **Why it's beneficial in isolation.** Be specific: removes silent
   duplication, surfaces preconditions at the boundary, names a
   recurring operation, makes illegal states unrepresentable.
   "Cleaner" is not a reason.
3. **Why it's a foundation for future work.** Which tickets, features,
   or known follow-ups become easier? Cite concrete artifacts. If you
   can't name a beneficiary, the refactor is speculative — see
   "Strive to add as little code as possible."
4. **Risk.** What could go wrong, and what guards against it? Note
   test counts at the boundary so the next reviewer can verify the
   same baselines you saw.

Two forms of the same reasoning: the **ticket comment (long form)**
with the four sections under headings, and the **commit message
(trimmed form)** — header under 70 chars, body 8–15 lines. Author the
long form first; the commit message is a trim of it, not the other
way around.

Avoid: "cleanup"/"refactor for clarity" with no specifics; listing
every helper extracted (the diff shows that); future-work claims with
no concrete beneficiary.

### Refactors that change documented architecture must update the doc

When a refactor changes a shape that a rules/architecture doc
describes (pipeline phases, module boundaries, protocol structure),
the doc update rides in the same PR. Doc-only catch-up PRs are fine
when drift is discovered later, but they're a sign the process
leaked.
