# Architecture constraints

Decided, binding architecture facts. STATE.md answers "what is in
flight"; this file answers "what must remain true." Every constraint
here was decided in a design dialog or by a hard external fact; a
change to any of them is a design decision requiring its own dialog
and a PR that updates this file in the same change (see
METHODOLOGIES.md "Refactors that change documented architecture").

The intuition: STATE.md is a working artifact you update aggressively;
this file is a contract you update deliberately.

Established 2026-10-29 (bootstrap dialog with the project owner).

## What this project is

`testcontainers-for-zio` publishes a family of Scala libraries under
`io.github.scottweaver` that expose Docker-backed test dependencies
(PostgreSQL, MySQL, Cassandra, Kafka, Solr) and migration aspects
(Flyway, Liquibase, cassandra-migration) as ZIO `ZLayer`s for use in
`zio-test` suites. Artifacts publish to Maven Central via Sonatype.

## Supported platforms

- **ZIO 2.x is the only effect runtime.** (2026-10-29) ZIO 1.x
  support is dropped. The `zio-1.x` modules (`mysql`, `postgresql`,
  `cassandra`, `kafka`, `db-migration-aspect`,
  `cassandra-migration-aspect`) and the `ZIOSeries.Series1X` build
  switch are to be deleted; no new 1.x work is accepted. Until the
  deletion lands, they remain in the aggregate but are frozen.
  Reason: the dual-series build doubles every module and the
  ecosystem has moved to 2.x.
- **Scala 2.13 and one Scala 3 line are the cross-build targets;
  Scala 2.12 is dropped.** (2026-10-29) `V.scala`,
  `crossScalaVersions`, the `build-all` command, and the CI matrix
  must agree on exactly these two lines; the CI matrix is the source
  of truth and `V.scala` reads it. Reason: 2.12 blocks dependency
  upgrades and no consumer has asked for it.
- **The Scala 3 line is 3.3 LTS until ZIO is bumped past 2.0.x.**
  (2026-10-30) ZIO 2.0.9's trace macro does not compile on Scala
  3.4+, and the shared 2.13 source needs `-source:3.3` for `with` as
  a type operator. When ZIO moves to 2.1.x (STATE.md "Next up"), the
  line moves to the current 3.x release and `-source:3.3` is
  revisited.
- **Source compiles for both dialects from one tree.** Only syntax
  shared by 2.13 and 3 is used (see SCALA_BEST_PRACTICES.md
  "Cross-building"). Dialect-specific code lives in
  `src/main/scala-2.13` / `src/main/scala-3` with identical public
  signatures.

## Container engine direction

- **`modules/core` (artifact `zillen`) is the project's own
  Docker Engine client and the intended replacement for
  testcontainers-scala.** (2026-10-29) It speaks the Docker Engine
  HTTP API directly over Netty (Unix socket / TCP) with zio-json
  models and zio-streams bodies, with no dependency on
  `org.testcontainers` or `com.dimafeng`. The direction is to migrate
  each container module from wrapping `com.dimafeng.testcontainers.*`
  to building on `zillen`, module by module, until
  testcontainers-scala is no longer a dependency. Reason: owning the
  engine removes the JVM testcontainers transitive tree and the
  slf4j/logback conflicts it drags in, and lets container lifecycle
  be fully ZIO-native (`Scope`, `Schedule`-driven readiness).
- **Until a module is migrated, it wraps testcontainers-scala
  unchanged.** A module is either on testcontainers-scala or on
  zillen, never both; the migration of a module is its own PR with
  its own design note. `postgresql-zio-2.0` already depends on `core`
  and is the first migration candidate.
- **zillen's public surface is the `Docker` facade plus the `Command`
  ADT.** Consumers build container modules against `Docker.*` /
  `Docker.cmd.*`; `Interpreter`, `NettyRequestHandler`, and the
  `netty` package are internal and may change without notice.

## Module layering

Dependencies flow downward only; no module imports from a layer above
it.

1. **`models`** (`zio-testcontainers-models`): ZIO-free shared value
   types (`JdbcInfo`). `needsZio := false` is binding; this module
   never gains a ZIO dependency so it can be shared with non-ZIO
   consumers.
2. **`core`** (`zillen`): the Docker Engine client. Depends on ZIO,
   zio-prelude, zio-json, zio-streams, Netty. Does not depend on
   `models` or any container module.
3. **Container modules** (`*-zio-2.0`: postgresql, mysql, cassandra,
   kafka, solr): one Docker-backed service each, exposed as a
   `Z*Container.live` layer with a `Settings` layer. Depend on
   `models` and either testcontainers-scala or `core`. Never depend on
   each other.
4. **Aspect modules** (`db-migration-aspect`, `liquibase-aspect`,
   `cassandra-migration-aspect`): `zio-test` aspects that run
   migrations against a container. Depend on `models` and on a
   container module only in `test->test` scope. Production code of an
   aspect must not depend on a specific container module; it works
   against `JdbcInfo` / a driver session provided by the environment.
5. **`docs`** (`docs-project`, not published): unidoc + Docusaurus.
   Depends on everything; nothing depends on it.

Adding a new container is a new module at layer 3 following the
`ZPostgreSQLContainer` shape; it is a routine addition, not an
architecture change.

## Public layer contract

Each container module's `live` layer provides, via
`ZLayer.scopedEnvironment`, the running container plus the
ready-to-use client handles a test needs (for JDBC containers:
`JdbcInfo`, `java.sql.Connection`, `javax.sql.DataSource`). The layer
requires exactly its `Settings`. Acquisition and release ride the
`Scope`; a test provides the layer with `provideLayerShared` so one
container serves the suite. Changing what a `live` layer provides or
requires is a published-API change and gets a MiMa check and a
release note.

## Build and release

- The build is sbt with the in-repo `ZioEcosystemProjectPlugin`
  (`project/`) providing `stdSettings`, `zioSeries`, `needsZio`, the
  `build` / `lint` / `fix` / `fmt` / `build-all` / `publish-all`
  composable commands, and the welcome banner. New modules opt in
  automatically (`trigger = allRequirements`).
- Versions are pinned in `project/V.scala` only. No version literal
  appears in `build.sbt` or a module.
- Publishing goes to Sonatype (`s01.oss.sonatype.org`) with sbt-pgp
  signing, triggered by a published GitHub release tag through
  `.github/workflows/ci.yml`. Credentials are GitHub Actions secrets
  (`SONATYPE_USERNAME`, `SONATYPE_PASSWORD`, `GPG_PRIVATE_KEY`,
  `GPG_PASSWORD`) read by `project/SonatypeCredentials.scala`; no
  credential is ever committed, and local publishing reads them from
  the environment only.
- Documentation is a Docusaurus site under `website/` fed by
  `docs/` and the unidoc output in `website/static/api`, published to
  GitHub Pages by the `docs` sbt project.

## Audit triggers

Files whose changes warrant re-checking this doc during post-merge
cleanup:

- `build.sbt` (aggregate list, `dependsOn` edges, module settings)
- `project/V.scala`, `project/build.properties`,
  `project/ZioEcosystemProjectPlugin.scala`, `project/Commands.scala`
- `.github/workflows/ci.yml` (Scala matrix, publish job)
- `modules/core/src/main/scala/io/github/scottweaver/zillen/Docker.scala`,
  `.../zillen/Command.scala`, `.../zillen/Interpreter.scala`
- `modules/models/**`
- any `modules/*/src/main/**/Z*Container.scala` (layer contract)
- `project/SonatypeCredentials.scala`

## Structural criteria

Structural (this doc must change in the same PR): adding or dropping a
Scala or ZIO target; a new `dependsOn` edge that violates the layering
above; a container module switching from testcontainers-scala to
zillen (or back); a change to what a `live` layer provides or
requires; a change to zillen's public facade or the `Command` ADT; a
change to where artifacts publish or how credentials reach the build;
replacing Netty or zio-json inside zillen.

Not structural (no update needed): a new container module that
follows the existing shape; new `Command`s added to the ADT; version
bumps in `V.scala` that stay within the supported lines; test
changes; new readiness checks; docs and website content.

## Open items

- **TBD (2026-10-29):** whether the ZIO 1.x modules are deleted in
  one PR or retired per container as each migrates to zillen.
- **TBD (2026-10-29):** whether `models` is folded into `core` once
  testcontainers-scala is gone, or stays separate for non-ZIO
  consumers.

## Maintenance

Update when a constraint above is deliberately renegotiated (design
dialog + PR updating this file), or when a recorded TBD is resolved.
Never for in-flight status; that's STATE.md. Keep constraints
falsifiable and dated. Secrets never enter this doc.
