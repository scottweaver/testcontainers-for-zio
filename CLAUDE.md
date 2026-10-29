# testcontainers-for-zio

A family of Scala libraries (`io.github.scottweaver`) that expose
Docker-backed test dependencies (PostgreSQL, MySQL, Cassandra, Kafka,
Solr) and migration aspects as ZIO 2.x `ZLayer`s for `zio-test`
suites. Built with sbt, cross-compiled for Scala 2.13 and Scala 3, and
published to Maven Central. The long-term goal is to replace the
testcontainers-scala dependency with the project's own Docker Engine
client, `zillen` (`modules/core`).

## Agent memory and rules

This file is the shared memory root for **all** agents working on the
project — Claude Code, Cowork, and any others. `AGENTS.md` is a
symlink to this file so tools reading either name get identical
content. Durable project knowledge belongs in this file or in
`.claude/rules/` — not in any tool-private memory store — so the
human and every agent can read and modify it.

All files in `.claude/rules/` are loaded each turn. Use this table to
decide which file is authoritative for a given decision.

| For decisions about...                                  | Authority                  |
| ------------------------------------------------------- | -------------------------- |
| Scala style, ZIO conventions, type discipline           | `SCALA_BEST_PRACTICES.md`  |
| Workflow: branching, PRs, refactors, post-merge cleanup | `METHODOLOGIES.md`         |
| Binding architecture constraints                        | `ARCHITECTURE.md`          |
| Bindings for portable skills (stand-up, wrap-up)        | `PROJECT.md`               |
| Current project state, in-flight work, what's next      | `STATE.md`                 |

`STATE.md` is the rehydration document: every agent session starts by
reading it. Stable architecture docs win over `STATE.md` on questions
of intended shape; `STATE.md` wins on what is actually in flight
right now.

**Non-Claude agents (Cursor, etc.):** `.cursor/rules/*.mdc` are
generated mirrors of `.claude/rules/` — edit the source, then run
`agent-sync` to regenerate; never edit the mirrors. Reusable
cross-project workflows ("skills") live at `~/.claude/skills/<name>/
SKILL.md`; any agent may read one and follow it when the user asks
for that workflow by name (e.g. wrap-up, daily-stand-up,
bootstrap-agent-rules).

## Top-level defaults

- Confirm before destructive operations (branch deletion, history
  rewrite, file deletion outside `/tmp`).
- Draft tickets in chat before creating them.
- New feature work starts on a new branch; PRs open only when the
  work moves the needle (see METHODOLOGIES.md).
- Non-trivial designs go through an interactive design dialog before
  implementation.
- Run `sbt build` (lint + strict compile + test) before pushing;
  container tests need a running Docker daemon.
