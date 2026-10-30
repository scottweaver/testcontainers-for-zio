# Project State

The rehydration document. Any agent starting a session reads this
first to learn where the project stands right now. It answers "where
are we" — never "how does this work" (that's ARCHITECTURE.md and the
code) and never "how should we work" (that's METHODOLOGIES.md).

Last updated: 2026-10-30

## Active workstream

Reviving the project after a long pause (last release 0.10.0 in Feb
2023; since then only PR #40, a postgres image-name fix, has landed).
The first step is the build modernization on branch
`chore/build-modernization` (issue #52): sbt 1.13.1, sbt-scalafix
0.14.9, cross-build exactly Scala 2.13.18 + 3.3.9, Scala 2.12
removed everywhere (`V.scala`, `ScalaCompilerSettings`, commands, CI
matrix), testcontainers-scala 0.43.6 with the MySQL default image
pinned to 8.4, plus a tree-wide `clean.sh`. The binding decisions from the
2026-10-29 bootstrap dialog are: ZIO 2.x only (1.x modules frozen,
slated for deletion), Scala 2.13 + current Scala 3, and `zillen`
(`modules/core`) is the intended replacement for testcontainers-scala.

## Branches in flight

| Branch | Purpose | Status |
|---|---|---|
| `main` | trunk | at `7305385` (rules layer bootstrap) |
| `chore/build-modernization` | issue #52: sbt / Scala / scalafix / CI modernization, drop 2.12, MySQL fix, per-test timeouts | PR #55 was **green** at `90dc1ed` (5.5 min); the docs-only push `0145ccd` then timed out at 30 min (see #56). Timeouts + pre-pull removal pushed; **awaiting a green re-run**, then owner review and merge; closes #52, #54 |
| `chore/remove-zio-1x` | delete the six ZIO 1.x modules + `ZIOSeries` switch; resolves the ARCHITECTURE TBD | PR #57, **stacked on #55** (base `chore/build-modernization`); CI pending; GitHub retargets to `main` once #55 merges |

Stale remote branches (`cassandra-support`, `circle-ci-work`,
`readme-cleanup`, `release-publishing`, `release/0.7.0`,
`staging/0.5.0`, `zio-2.0`) predate 0.10.0 and are candidates for
deletion once confirmed merged.

## Next up

1. Confirm PR #55 is green again, review, merge; then `/wrap-up 55`.
   Release note: MySQL `Settings.default` image moved from `latest`
   to `8.4`.
2. Triage the five pre-revival community PRs: #51 (testcontainers-scala
   0.44.1, superseded by the 0.43.6 choice; close or fold into the
   0.44 migration), #43 and #45 (ZIO / zio-kafka bumps, overlap with
   item 4), #41 (LocalStack, issue #42) and #32 (k3s) as new layer-3
   modules once the build is stable.
3. Merge PR #57 (ZIO 1.x removal) after #55; `/wrap-up 57`.
   Then parallelise the remaining container suites (plan drafted
   2026-10-30: raise sbt's `ForkedTestGroup` limit, unique container
   names and ephemeral ports in the zillen suites, smaller test heap).
4. Bump ZIO to 2.1.x (plus zio-json, zio-prelude, zio-kafka) in
   `V.scala`; then move the Scala 3 line from 3.3 LTS to the current
   3.x and drop `-source:3.3` if `with` is gone. testcontainers-scala
   0.44 (testcontainers-java 2.0) is a separate package-rename
   migration.
5. Fix #56 (zillen Docker Engine requests have no timeout; the
   `makeScopedContainer` release can await a removed container).
   Small, self-contained, and a prerequisite for trusting zillen in
   CI.
6. Plan the first zillen migration (`postgresql-zio-2.0` already
   depends on `core`): design note, then PR.
7. Prune stale remote branches; update README artifact coordinates for
   the next release.

## Most recent meaningful progress

- **2026-10-30 (late) — ZIO 1.x removed (PR #57).** Six frozen
  modules, the `ZIOSeries` switch, zio-magic and the 1.x zio-kafka line
  deleted; one `V.zioVersion`. Also fixed the ZIO 2 cassandra aspect
  pinning zio-test to 1.x. Why: half the CI test phase ran frozen
  code. Risk: stacked on #55; merge order matters. Found while doing
  it: the Liquibase aspect's `build.sbt` path does not match its source
  directory, so that module compiles nothing (loose end below).
- **2026-10-30 (late) — CI timeout diagnosed, guarded (#56).** The
  docs-only push to PR #55 ran CI anyway and both test jobs hit the
  30 min job limit: `ContainerSpec` in `core` hung after its container
  stopped, and the coursier cache failed to restore ("Egress is over
  the account limit"), adding ~10 min. Added `TestAspect.timeout(5
  minutes)` to every ZIO 2 container suite, dropped the pre-pull step
  (measured as a wash), filed #56 for the zillen hang (no HTTP request
  timeout in zillen). Why: a flake must cost one test, not the whole
  job. Risk: the timeout only bounds the symptom; #56 is the fix.
- **2026-10-30 — PR #55 made CI-viable.** Runner moved off retired
  `ubuntu-20.04`; Scala 3 line set to 3.3 LTS because ZIO 2.0.9's
  trace macro fails on 3.4+ (ARCHITECTURE.md updated); `-source:3.3`
  keeps `with` legal in shared source; testcontainers-scala 0.43.6
  and MySQL default image 8.4 fix the MySQL suites (#54). Risk: the
  8.4 pin is a published-default change and needs a release note;
  Scala 3.10 returns only with the ZIO bump. Later the same day: CI
  matrix parallelised, docs-only paths ignored, run went from ~10 min
  to 5.5 min; first fully green run.
- **2026-10-29 — Build modernization branched (issue #52).** sbt
  1.13.1, sbt-scalafix 0.14.9, Scala 2.13.18 only on 2.x, 2.12
  removed from every build file and the CI matrix. Why: on JDK 21 the
  old 2.13.8 compiler could not even build the Zinc bridge, so nothing
  compiled.
- **2026-10-29 — Rules layer bootstrapped.** Added `CLAUDE.md`
  (+ `AGENTS.md` symlink) and `.claude/rules/` with
  SCALA_BEST_PRACTICES, METHODOLOGIES, ARCHITECTURE, PROJECT, and this
  file; generated `.cursor/rules/` mirrors. Why: the project is being
  revived after 2.5 years and agents need durable context on the
  ZIO-2-only / Scala 2.13+3 / zillen-direction decisions. Risk: the
  ARCHITECTURE constraints were set in one dialog and have not yet
  been exercised by a real PR; the two TBDs there are open.

## Blocked / waiting

- PR #55 waits on owner review and merge.
- A fully green local `sbt build` waits on #53 (zillen socket path);
  CI runners have `/var/run/docker.sock` so CI is unaffected. Locally
  the other suites pass with
  `DOCKER_HOST=unix://$HOME/.docker/run/docker.sock`,
  `TESTCONTAINERS_DOCKER_CLIENT_STRATEGY=org.testcontainers.dockerclient.EnvironmentAndSystemPropertyClientProviderStrategy`
  and `TESTCONTAINERS_RYUK_DISABLED=true` (Ryuk mounts
  `/var/run/docker.sock`, which does not exist here).

## Session handoff
<!-- transient; owned by the checkpoint skill -->

**Resume here:** confirm CI is green on PR #55 (timeouts + pre-pull
removal) and on PR #57 (ZIO 1.x removal, stacked on #55). Merge #55,
`/wrap-up 55`; then merge #57, `/wrap-up 57`. Next: the test
parallelisation plan ("Next up" item 3) or #56.

Decisions made 2026-10-30, all recorded in PR #55 / ARCHITECTURE.md:
- Scala 3 line is 3.3 LTS, not 3.10, until ZIO leaves 2.0.x
  (ZIO 2.0.9 trace macro fails on 3.4+; `-source:3.3` for `with`).
- testcontainers-scala 0.43.6, not 0.44.1: 0.44 drags testcontainers-
  java 2.0 package renames across every module (own PR).
- MySQL default image pinned to `8.4` (testcontainers 1.21 my.cnf uses
  `innodb_log_file_size`, removed in MySQL 26). Published-default
  change; release note owed.
- CI tuning rode PR #55 rather than a stacked branch because it only
  touched the workflow file the PR already rewrote.

Open questions for the owner:
- `paths-ignore` skips docs-only PRs entirely; if branch protection
  with required checks is ever added, revisit.
- GitHub Actions cache egress for the account is exhausted ("Egress
  is over the account limit" on 2026-10-30; the repo holds ~1.9 GB in
  9 caches). Runs re-download dependencies until the quota resets.
  Consider pruning old caches or accepting slower runs for now.

Loose ends noticed, not acted on:
- `build.sbt` points `liquibaseAspect` at
  `modules/zio-2.0-liquibase-aspect` (only a stale `target/`), while
  the tracked sources live in `modules/liquibase-aspect-zio-2.0`. The
  module compiles nothing and `LiquibaseAspectSpec` never runs. Fix the
  path, then expect the suite to need work (it has not run in years).
- `paths-ignore` on `pull_request` is evaluated against the whole PR
  diff, not the latest push, so a docs-only commit on a mixed PR still
  runs CI. The carve-out only skips PRs that are docs-only end to end.
- The pre-pull step was dropped on 2026-10-30; it had been measured as
  a wash (cost 29-50 s, saved ~20 s).
- Two pre-fix workflow runs may still show "queued" (the `main` push
  `7305385` and PR run `40984606588`); cancel by hand, they target the
  retired `ubuntu-20.04` image and will never start.
- Local `~/.testcontainers.properties` pins
  `UnixSocketClientProviderStrategy`; combined with Docker Desktop's
  default-socket setting off, local runs need the env vars listed in
  "Blocked / waiting". Enabling "Allow the default Docker socket" in
  Docker Desktop would remove the need, and would also unblock #53's
  symptoms locally without code.
- `ci.yml` publish job still reads `secrets.GPG_PASSPHRASE` while
  ARCHITECTURE.md names `GPG_PASSWORD`; verify which secret exists
  before the next release.
- Scala 3 `-source:3.3` and `-Werror` live in `ScalaCompilerSettings`;
  `scalafixDependencies` still carries `scaluzzi` 0.1.23, unverified
  against scalafix 0.14 rules beyond "lint passes".

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
