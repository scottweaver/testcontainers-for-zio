# Project State

The rehydration document. Any agent starting a session reads this
first to learn where the project stands right now. It answers "where
are we" — never "how does this work" (that's ARCHITECTURE.md and the
code) and never "how should we work" (that's METHODOLOGIES.md).

Last updated: 2026-11-04

## Active workstream

Reviving the project after a long pause (last release 0.10.0 in Feb
2023). The revival's first wave landed on `main` on 2026-10-30: build
modernization (#55: sbt 1.13.1, Scala 2.13.18 + 3.3.9, 2.12 dropped,
testcontainers-scala 0.43.6, MySQL default image 8.4), the zillen
ready-check hang fix (#58, issue #56), ZIO 1.x removal (#61), parallel
container suites (#59) and the Liquibase module revived (#60). CI is
green at about 2.5-3 min per run (was 5.5, and 30-min timeouts before
that). Binding decisions from the 2026-10-29 bootstrap dialog hold: ZIO
2.x only, Scala 2.13 + one Scala 3 line,
and `zillen` (`modules/core`) is the intended replacement for
testcontainers-scala. Community PR triage is complete: #51 is closed;
#43 and #45 are closed with attribution after their replacement landed
in #63. ZIO 2.1, Scala 3.10, and Dependabot are on `main`; CI is green.
Next: review the initial automated updates, prepare a release, and plan
the first zillen migration.

## Branches in flight

| Branch | Purpose | Status |
|---|---|---|
| `main` | trunk | at `cc4f9da` (#63 merged; lint and both Scala test jobs green) |
| `dependabot/*` | automated dependency updates | #64–#71 triaged; replacement repair in progress |
| `codex/ci-dependency-fixes` | consolidated dependency compatibility and CI repair | `sbt build` passes; Scala 3 and hosted CI pending |

Stale remote branches (`cassandra-support`, `circle-ci-work`,
`readme-cleanup`, `release-publishing`, `release/0.7.0`,
`staging/0.5.0`, `zio-2.0`) predate 0.10.0 and are candidates for
deletion once confirmed merged.

## Next up

1. Finish the consolidated CI repair on `codex/ci-dependency-fixes`.
   It incorporates #64–#68, #70, and #71 with paired logging versions,
   Flyway's MySQL test module, Liquibase classpath-prefix compatibility,
   and two concurrent CI container suites. Verify local build and both
   hosted Scala jobs before merging; close superseded bot PRs afterward.
   #69 (Bloop 2.1.3) is excluded: its green CI did not run the full
   `sbt build` clean phase, which failed locally with missing build
   definition classes. Investigate separately rather than merge on green
   compile/test alone. No retries or test exclusions were added.
2. Preserve #41 (LocalStack, issue #42) and #32 (k3s) for modernization
   as new layer-3 modules; neither feature exists on `main`. Remove
   obsolete build wiring and ZIO 1 support when adapting them.
   #51 is closed: testcontainers-scala 0.44 (testcontainers-java 2.0)
   remains a separate coordinated package/API migration, not a landed
   upgrade.
3. Cut release 0.11.0. Release notes owed: MySQL `Settings.default`
   image `latest` → `8.4`; ZIO 1.x artifacts discontinued at 0.10.0;
   Liquibase aspect published again; Scala 2.12 dropped; ZIO 2.1,
   zio-kafka 3, and Scala 3.10 compatibility changes. Verify the
   `GPG_PASSPHRASE` / `GPG_PASSWORD` secret name first (loose ends).
4. zillen hardening from #56: a per-request HTTP timeout in
   `NettyRequestHandler`, surfaced as a typed failure.
5. Plan the first zillen migration (`postgresql-zio-2.0` already
   depends on `core`): design note, then PR.
6. Prune stale remote branches; update README artifact coordinates for
   the next release.

## Most recent meaningful progress

- **2026-11-04 — Dependabot triage exposed shared CI and logging gaps.**
  Five unrelated PRs hit Cassandra's two-second DDL timeout; #68 repeated
  it on rerun, while #71 passed. #65/#66 separately disable logging,
  #67 lacks the Flyway MySQL module, and #70 fails classpath-prefix
  resolution. Consolidated repair passes local `sbt build`; Bloop remains
  separate after a local clean-phase failure. Risk: reduced concurrency
  must still be verified on hosted runners.

- **2026-11-04 — #63 merged; #43/#45 closed with contributor credit.**
  ZIO ecosystem upgrade and weekly dependency automation are on `main`.
  PR lint and both Scala jobs passed (~3 minutes); post-merge CI also
  passed. Hosted Dependabot runs succeeded and opened #64–#71, including
  verified centralized-version edits. Risk: several proposed dependency
  updates fail CI and need individual review; they are not part of #63.

- **2026-11-04 — ZIO upgrade and Dependabot implemented locally.**
  `sbt build` and Scala 3.10 strict compile/test both pass locally. Kafka 3 requires instance accessors and explicit earliest
  offset retrieval; Solr's upgraded HTTP test scopes the request.
  Dependabot supports literal `V.scala` references but not computed
  Scala matrix versions. Risk: Kafka 3 and Scala 3.10 affect consumer
  compatibility; documented for release. #43/#45 stay open until merge.

- **2026-11-04 — Community PRs triaged; #51 closed with explanation.**
  The version-only 0.44.1 bump does not cover the coordinated migration;
  0.43.6 remains intentional. #43/#45 still contain unapplied changes
  and are next, with Dependabot setup explicitly included by the owner.
  #41/#32 remain open as useful new features. Risk: dependency automation
  must be checked for actual sbt / `V.scala` coverage before claiming it
  prevents drift.

- **2026-10-30 — Revival stack landed on `main` (#55, #58, #61, #59,
  #60).** Merged in that order with merge commits. #57 was closed by
  GitHub when its base branch was deleted and continued as #61; the
  later PRs were retargeted to `main` before their base branches were
  removed. CI per run: 5.5 min → ~2.5 min; the zillen hang is gone
  (#56 closed). Why: one coherent wave, each PR green on its own.
  Risk: PR #59's 4-way parallelism has only a few green runs behind
  it; if Cassandra or Kafka flake under contention, lower
  `SBT_FORKED_TEST_GROUPS` before adding retries.
- **2026-10-30 (late) — #56 root-caused and fixed (PR #58); suites
  parallelised (PR #59).** The hang: `ReadyCheckLive` forked a poll
  whose inspect failure left the promise incomplete, and the release
  polled after a forced remove (404). Fixed with a deterministic
  `ReadyCheckSpec`. Parallel: sbt `ForkedTestGroup` limit 4, 1 GB test
  heap, Docker-assigned names for `PostgresContainer`. Why: 2 of 3 CI
  runs hung; the serial test phase was most of the job. Risk: the
  stack is four PRs deep (#55 ← #58, #55 ← #57 ← #59); merge order
  matters and #57 needs `main` merged in after #55 lands.
- **2026-10-30 (late) — ZIO 1.x removed (PR #57, landed as #61).** Six frozen
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
## Validation of #63

- `sbt build`: lint, strict compilation, and Scala 2.13 tests passed
  (25 passed, 1 existing ignored test).
- `sbt '++3.10.0!' enableStrictCompile test`: strict compilation and
  Scala 3 tests passed (23 passed, 1 existing ignored test). Kafka's existing
  tests remain under `src/test/scala-2`, so their runtime coverage is on
  Scala 2.13; the Kafka production module compiles on both.
- Dependabot YAML parses; version/property resolution and ignore-range
  support checked against upstream sbt parser. First hosted sbt and
  Actions runs succeeded; #64 updates `project/V.scala` correctly. Rule
  mirrors and diff whitespace checked.

## Blocked / waiting

- Docker Desktop's default socket was enabled on 2026-11-04 and
  `/var/run/docker.sock` now exists. The daemon restart interrupted
  an initial test run; restarted builds passed. This removes the local
  symptom of #53, not the underlying hard-coded socket behavior.

## Session handoff
<!-- transient; owned by the checkpoint skill -->

**Resume here:** finish CI validation of `codex/ci-dependency-fixes`.
#63/#72 are merged. The repair replaces seven Dependabot updates while
preserving logging and migration behavior. Keep #69 open pending its
local `sbt build` clean-phase investigation. #41/#32 remain deferred.

Decisions made 2026-10-30, all recorded in PR #55 / ARCHITECTURE.md:
- Historical Scala 3 restriction: 3.3 LTS while on ZIO 2.0.x. The
  merged upgrade uses 3.10 with ZIO 2.1; `-source:3.3` remains.
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
- `PROJECT.md` had `wrapup.audit_doc: null` although ARCHITECTURE.md
  carries an audit-trigger list; bound on 2026-10-30 so `wrap-up`
  audits it automatically from now on.
- `modules/zio-2.0-liquibase-aspect/` is an untracked stale `target/`
  left from the wrong `build.sbt` path (fixed in PR #60); `clean.sh` or
  `rm -rf` it locally.
- `paths-ignore` on `pull_request` is evaluated against the whole PR
  diff, not the latest push, so a docs-only commit on a mixed PR still
  runs CI. The carve-out only skips PRs that are docs-only end to end.
- Local `~/.testcontainers.properties` pins
  `UnixSocketClientProviderStrategy`; combined with Docker Desktop's
  default-socket setting previously off, local runs needed overrides.
  The default socket is now enabled; #53 still tracks portable discovery.
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
