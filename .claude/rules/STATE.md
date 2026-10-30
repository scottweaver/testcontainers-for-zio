# Project State

The rehydration document. Any agent starting a session reads this
first to learn where the project stands right now. It answers "where
are we" — never "how does this work" (that's ARCHITECTURE.md and the
code) and never "how should we work" (that's METHODOLOGIES.md).

Last updated: 2026-10-29

## Active workstream

Reviving the project after a long pause (last release 0.10.0 in Feb
2023; since then only PR #40, a postgres image-name fix, has landed).
The first step is the build modernization on branch
`chore/build-modernization` (issue #52): sbt 1.13.1, sbt-scalafix
0.14.9, cross-build exactly Scala 2.13.18 + 3.10.0, Scala 2.12
removed everywhere (`V.scala`, `ScalaCompilerSettings`, commands, CI
matrix), plus a tree-wide `clean.sh`. The binding decisions from the
2026-10-29 bootstrap dialog are: ZIO 2.x only (1.x modules frozen,
slated for deletion), Scala 2.13 + current Scala 3, and `zillen`
(`modules/core`) is the intended replacement for testcontainers-scala.

## Branches in flight

| Branch | Purpose | Status |
|---|---|---|
| `main` | trunk | at `7305385` (rules layer bootstrap) |
| `chore/build-modernization` | issue #52: sbt / Scala / scalafix modernization, drop 2.12 | PR open; `sbt lint` green on 2.13.18 and 3.10.0; test failures remaining are pre-existing (#53, #54) |

Stale remote branches (`cassandra-support`, `circle-ci-work`,
`readme-cleanup`, `release-publishing`, `release/0.7.0`,
`staging/0.5.0`, `zio-2.0`) predate 0.10.0 and are candidates for
deletion once confirmed merged.

## Next up

1. Land `chore/build-modernization` (issue #52) once CI is green on
   the new matrix; then wrap-up.
2. Remove the ZIO 1.x modules and the `ZIOSeries` switch per
   ARCHITECTURE.md (decide: one PR or per-container as each migrates).
3. Bump ZIO 2.x and testcontainers-scala to current releases in
   `V.scala`; verify every `*-zio-2.0` suite is green.
4. Plan the first zillen migration (`postgresql-zio-2.0` already
   depends on `core`): design note, then PR.
5. Prune stale remote branches; update README artifact coordinates for
   the next release.

## Most recent meaningful progress

- **2026-10-29 — Build modernization branched (issue #52).** sbt
  1.13.1, sbt-scalafix 0.14.9, Scala 2.13.18 + 3.10.0 only, 2.12
  removed from every build file and the CI matrix. Why: on JDK 21 the
  old 2.13.8 compiler could not even build the Zinc bridge, so nothing
  compiled. Risk: CI has not yet run on the new matrix; `ci.yml` still
  uses old action versions. Local `sbt build` still fails in `core`
  and `postgresql-zio-2.0` (zillen hardcodes `/var/run/docker.sock`,
  #53) and in the MySQL suites (`mysql:latest` rejects
  `--skip-host-cache` from testcontainers 1.17, #54); neither is
  caused by this branch.
- **2026-10-29 — Rules layer bootstrapped.** Added `CLAUDE.md`
  (+ `AGENTS.md` symlink) and `.claude/rules/` with
  SCALA_BEST_PRACTICES, METHODOLOGIES, ARCHITECTURE, PROJECT, and this
  file; generated `.cursor/rules/` mirrors. Why: the project is being
  revived after 2.5 years and agents need durable context on the
  ZIO-2-only / Scala 2.13+3 / zillen-direction decisions. Risk: the
  ARCHITECTURE constraints were set in one dialog and have not yet
  been exercised by a real PR; the two TBDs there are open.

## Blocked / waiting

- A fully green local `sbt build` waits on #53 (zillen socket path)
  and #54 (testcontainers bump for MySQL). Cassandra, Kafka, Solr,
  and the 1.x postgres suites pass with
  `DOCKER_HOST=unix://$HOME/.docker/run/docker.sock` and
  `TESTCONTAINERS_DOCKER_CLIENT_STRATEGY=org.testcontainers.dockerclient.EnvironmentAndSystemPropertyClientProviderStrategy`.

## Maintenance

- **Refresh trigger:** any merge or milestone that changes what an
  incoming agent needs to know: workstream shifts, a branch opens or
  closes, "Next up" changes, something lands. Wired into
  METHODOLOGIES.md's post-merge routine (the "refresh STATE.md"
  step).
- **Always update:** "Last updated"; "Branches in flight"; prepend a
  progress entry (what / why / risk voice — a judgment edit, not a
  paste of the PR description).
- **As applicable:** "Active workstream" paragraph, "Next up",
  "Blocked / waiting".
- **Trim policy:** progress log holds at most 10 entries — drop the
  oldest when adding. Anything stable graduates out of this file
  into the appropriate rules doc; this file stays small because
  every agent loads it every turn.
- **Edit policy:** STATE.md is authored on feature branches,
  propagates through merges, and is refreshed (not deleted) on new
  branches. Never edit it directly on `main`; docs-only diffs under
  `.claude/rules/` ride the METHODOLOGIES.md docs-only carve-out.
- **Keep entries short:** each progress entry is a pointer — date,
  PR #, ticket, a sentence or two of judgment. If you're tempted to
  write more, the detail belongs in the PR, commit, or ticket.
