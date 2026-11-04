# Dependency updates

Dependabot checks sbt dependencies and GitHub Actions weekly on Monday
at 09:00 America/Chicago. ZIO, Netty, and Testcontainers updates are
grouped by family; Actions updates form a separate group. Updates go
through normal PR review and the two-version CI matrix; no auto-merge
is configured.

The sbt ecosystem supports `build.sbt`, `project/plugins.sbt`,
`project/build.properties`, and `project/*.scala`. Its parser resolves
dotted version references such as `V.zioVersion` to literal values in
`project/V.scala`. Keep those versions centralized. Dependencies in
the build plugin are also in the scanned `project/*.scala` files.

Coverage limits:

- Scala versions are computed from the CI YAML matrix. Dependabot does
  not evaluate that code or update the matrix; maintain both matrix
  entries and the matching `V.scala` lookup/fallback values manually.
- Cross-built dependency discovery defaults to Scala 2.13 when it cannot
  resolve the computed Scala version. CI must verify Scala 3 artifacts
  and compatibility too.
- Testcontainers Scala 0.44+ is intentionally ignored pending the
  coordinated Testcontainers Java 2 package/API migration. Remove the
  ignore rule when that migration lands; patches on 0.43 remain eligible.
- Configuration takes effect on the default branch. After merge, inspect
  the first Dependabot job and verify that the ZIO dependencies and their
  `V.scala` edits are discovered. Static parser inspection is not a
  substitute for that hosted run.

References:

- [Supported sbt manifests](https://docs.github.com/en/code-security/reference/supply-chain-security/supported-ecosystems-and-repositories#sbt)
- [Dependabot sbt parser](https://github.com/dependabot/dependabot-core/blob/main/sbt/lib/dependabot/sbt/file_parser.rb)

The ZIO upgrade carries forward the Kafka API migration identified in
[#45](https://github.com/scottweaver/testcontainers-for-zio/pull/45)
by Zsolt Takacs, adapted to zio-kafka 3. The ZIO/Prelude upgrade was
also proposed in [#43](https://github.com/scottweaver/testcontainers-for-zio/pull/43)
by Ivo Petkovic. Recheck both contributions against the final build and
close them with attribution after the replacement lands.

## Release compatibility notes

The pending upgrade uses ZIO 2.1.27, zio-json 1.1.1, zio-prelude RC48,
and zio-kafka 3.8.1. Scala 3 artifacts are built with 3.10.0; consumers
must use a compiler that can read that TASTy version. Scala 2.13 remains
supported. zio-http 3.11.6 is used only by the Solr tests.

Kafka consumers must align their zio-kafka dependency with the new major
version. Its producer/consumer operations are called on service instances,
and bootstrap servers are exposed through settings properties. The removed
`withPerPartitionChunkPrefetch` override is no longer applied; the new
consumer uses the library's default fetch strategy. Earliest-offset
retrieval is now explicit to preserve consumption of records produced
before subscription. Do not describe this
as binary-compatible with zio-kafka 2.
