# Scala conventions

Conventions for Scala code in this project. For workflow (branching,
PRs, refactors) see METHODOLOGIES.md. For binding architecture facts
(module layering, supported Scala/ZIO versions) see ARCHITECTURE.md.

These are constraints, not suggestions. The tighter the rules, the
less time is spent re-deciding the same questions and the more
consistent the codebase becomes. When a rule below conflicts with the
quick-and-natural way of writing something, the rule wins.

The overall posture: functional Scala, with ZIO 2.x as the effect
system and application framework. Values in, values out, immutability
by default, effects described as `ZIO` values and run only at the
edge. Code compiles for both Scala 2.13 and Scala 3, so the subset
used is the one both dialects share.

## ZIO is the framework

### Every effect is a `ZIO` value; nothing runs until the edge.

Side effects (Docker calls, JDBC connections, Netty channels, file IO,
clocks, randomness) are described with `ZIO.attempt`,
`ZIO.attemptBlocking`, `ZIO.acquireRelease`, and friends, never
executed inline. The only places that run an effect are the `ZIOApp`
entry point, a `ZIOSpecDefault`, and the sbt plugin code under
`project/`. `Unsafe.unsafe { ... }` / `Runtime.default.unsafe.run`
appear nowhere in `modules/**/src/main`.

### Services are traits; implementations are `final case class`es; wiring is a `ZLayer`.

The pattern in `Interpreter` / `InterpreterLive` is the project
pattern:

```scala
trait Interpreter {
  def run(command: Command): DockerIO[Any, command.Response]
}

object Interpreter {
  val layer = ZLayer.fromZIO(ZIO.serviceWith[NettyRequestHandler](InterpreterLive.apply))
  def run(command: Command) = ZIO.serviceWithZIO[Interpreter](_.run(command))
}

final case class InterpreterLive(nettyRequest: NettyRequestHandler) extends Interpreter
```

- The trait is the contract. It has no implementation and no
  companion state.
- The live implementation is a `final case class` whose constructor
  parameters are its dependencies, so `ZLayer.fromFunction` /
  `ZLayer.derive` can build it and tests can instantiate it directly.
- The companion exposes a `layer` and accessor methods
  (`ZIO.serviceWithZIO`) so callers never reach into the environment
  by hand.
- Configuration is its own `final case class Settings` with a
  `Settings.default` layer (see `ZPostgreSQLContainer.Settings`).
  Layers read settings from the environment; they do not take them as
  constructor arguments.

### Resources are `Scope`d; nothing leaks.

Anything with a lifecycle (a container, a JDBC `Connection`, a Netty
channel, a Kafka client) is acquired with `ZIO.acquireRelease` and
exposed through `ZLayer.scoped` / `ZLayer.scopedEnvironment`. The
release action is total: it uses `.ignoreLogged` (or `.orDie` when
failure to release is a defect), never a bare `ZIO.attempt` whose
failure would be silently discarded by the finalizer machinery.

Test suites share one container per suite with
`provideLayerShared` / `provideShared`, never one per test case. This
is both a performance rule and the cheapest guard against orphaned
containers.

### Environment, error, and success channels are all designed.

`ZIO[R, E, A]` has three slots; each is chosen deliberately.

- `R` names only the services this effect actually uses. `R = Any`
  for pure computations and for effects whose dependencies were
  captured at construction.
- `E` is a purpose-built error type (see "Error handling"). `E =
  Throwable` is permitted only at the boundary with a Java library
  that throws, and is narrowed with `.refineOrDie` / `.mapError`
  before the effect leaves the module. `E = Nothing` is a claim that
  the effect cannot fail; make it only when `.orDie` was a conscious
  "this is a defect" decision, not a convenience.
- Type aliases like `DockerIO[R, A]` are the project's way of fixing
  `E` for a domain. Add one per error domain; don't re-spell the full
  `ZIO[R, DockerContainerFailure, A]` at every call site.

### ZIO combinators over hand-rolled control flow.

Retries are `Schedule`s, not loops. Timeouts are `.timeout` /
`.timeoutFail`, not `Thread.sleep` polling. Concurrency is
`ZIO.foreachPar`, `.zipPar`, `.race`, `Fiber`, `Queue`, `Ref`, `Hub`,
never `java.util.concurrent` primitives, `synchronized`, or
`@volatile`. Readiness checks (see `ReadyCheck`) are `Schedule`-driven
effects, not blocking spin-loops.

### Streaming data is a `ZStream`.

Anything that is naturally a sequence over time (Docker event
streams, chunked HTTP bodies, Kafka records, log tails) is a
`ZStream`, consumed with stream combinators. Collecting a whole stream
into memory is a deliberate choice made at the consumer, never inside
the producer.

## Type discipline

The type system is the design surface. Signatures are written first;
the implementation is constrained to fit them.

### Illegal states are unrepresentable.

A value that goes through phases (requested, created, running,
stopped) is a `sealed trait` with one `final case class` per phase.
A case class whose fields are mostly `Option` is an ADT that hasn't
been admitted yet. Scala 3 `enum`s are not used because the code
must also compile on 2.13; `sealed trait` + `final case class` /
`case object` is the ADT form on both dialects.

### Every `match` on our own sealed types is exhaustive. No `case _`.

The compiler's exhaustiveness check is the cheapest correctness tool
Scala gives us. A wildcard arm on one of our own sealed types turns
it off and silently absorbs every variant added later. For foreign
types that are not sealed, a catch-all is forced; make it loud
(fail the effect, log at warn) rather than a silent no-op.

### Identifier-shaped values are newtypes.

`ContainerId`, `ContainerName`, `Port`, `Image` exist for this
reason. A new identifier domain gets a new type. The project uses
zio-prelude `Newtype` / `Subtype` (with `Assertion`s where the
format has an invariant) in `core`; a plain `final case class
X(value: String) extends AnyVal` is acceptable in modules that don't
depend on zio-prelude. Either way, the smart constructor is the only
place the format is checked, and a typed value in hand is proof the
check passed.

### Wide types are not allowed in signatures.

`String`, `Int`, `Boolean`, `Any`, `Throwable` in a public signature
are each examined to see if something more specific is meant. It
usually is: an image name, a port number, a readiness state, an error
ADT. Two `Boolean` parameters in a row is a sealed trait asking to
exist.

### Parse, don't validate.

Data crossing a boundary (Docker Engine JSON, JDBC metadata,
environment variables, `Settings`) is decoded into a typed value once
at the boundary with a `JsonDecoder` / smart constructor that returns
an error, and never re-checked downstream. zio-json derived codecs on
case classes are the boundary contract for the Docker API; add a
model under `zillen/models` rather than threading `ast.Json` through
logic.

### Variance and type members are used where they buy safety.

`Command { type Response }` is the project's way of tying a request
to its response type so the interpreter can return
`command.Response`. Prefer this (or a typeclass with an output type)
to parallel hierarchies kept in sync by hand. Keep the machinery
simple: a dependent type member or a plain typeclass is the default;
higher-kinded or path-dependent towers need an argued case.

## Functional style

### Pure core, effectful shell.

Encoding, decoding, request construction, readiness decisions, and
model transformations are pure functions on immutable values. The
`ZIO` shell (interpreter, Netty handler, layer constructors) calls
into them. A pure function is tested with plain `assertTrue`; an
effectful one with `ZIOSpecDefault` and test services (`TestClock`,
`TestRandom`), not with sleeps.

### Immutability is the default.

`val` everywhere. `var` is permitted only as a local inside a
function whose result is immutable, and is a review flag. Shared
mutable state is a `Ref` / `Ref.Synchronized` / `FiberRef`, never a
`var` or a mutable collection reachable from more than one fiber.
Collections are the immutable `scala.collection.immutable` ones;
`scala.collection.mutable` appears only inside a function body and
never escapes it.

### Expressions over statements; combinators over loops.

`for`-comprehensions over `ZIO` and over collections; `map` /
`flatMap` / `foldLeft` / `collect` over `while` and indexed loops.
A `for` over effects that needs an intermediate `val` uses `=` bindings
inside the comprehension, not a `var` outside it (scalafix
`NoValInForComprehension` is on). Early return (`return`) is never
used; scalafix `DisableSyntax` and the `-Xlint:nonlocal-return` flag
enforce it.

### Data and functions, not class hierarchies.

Types are `final case class`es and `sealed trait`s; behaviour is
functions in companion objects and `*Ops` traits mixed into a facade
object (the `Docker extends ContainerOps with ModelOps with
FailureOps with ReadyCheckOps` pattern). No abstract classes with
protected mutable state, no inheritance for code reuse, no
`Serializable` hacks. Typeclasses (`JsonEncoder`, `JsonDecoder`,
project-defined ones) are the polymorphism mechanism; they are
resolved implicitly / via `given` and never passed by hand.

### Implicits are narrow and local.

Implicit conversions are not used; `-language:implicitConversions`
is intentionally absent from the compiler flags. Extension methods
are `implicit class X(val self: T) extends AnyVal` (compiles on both
dialects). Typeclass instances live in the companion of the type or
the typeclass so they are found without imports; orphan instances in
`package` objects are a review flag.

## Error handling

### Errors are values in the `E` channel, not exceptions.

Each module has one sealed error ADT (`DockerContainerFailure` is the
model). Variants carry enough data for a caller to `match` and do
something different per variant; a variant that is just a wrapped
`String` message is a wide type wearing an ADT costume.

### Defects are `.orDie`; failures are typed.

Exceptions from Java libraries are caught at the call site with
`ZIO.attempt` / `ZIO.attemptBlocking` and immediately classified:
`refineToOrDie[SpecificException]` + `mapError` into the module ADT
for recoverable failures, `.orDie` for situations that can only be
bugs or environmental impossibilities (Docker daemon absent, driver
class missing). Container `start()` failing is `.orDie` today because
there is no meaningful recovery; a readiness timeout is a typed
failure because the caller may retry with a longer `Schedule`.

### Boundaries classify; the interior trusts.

After the boundary has classified an error into the ADT, interior
code matches on the ADT exhaustively. No `catch`, no `Try`, no
`Either[Throwable, _]` past the boundary.

## Cross-building

- The code compiles on Scala 2.13 and Scala 3 from the same source.
  Use only syntax both accept: braces, `implicit` (not `given` /
  `using`), `implicit class` extension methods (not `extension`), no
  `enum`, no top-level definitions, no `export`, no significant
  indentation (`-noindent` is set for Scala 3).
- Dialect-specific code goes in `src/main/scala-2.13` /
  `src/main/scala-3` source directories with an identical public
  signature (see `VersionSpecific.scala` in `core`), and only when
  there is no shared-dialect form.
- Macro-based libraries are avoided unless they publish for both
  dialects; zio-json's derivation is the approved exception.

## Comments and documentation

Comments explain **why**, not **what**. Order of preference: rename,
extract, delete, comment. An inline comment that narrates a block is
a function waiting for a name. The deletion test: would removing the
comment lose something a competent reader cannot recover from the
code, names, tests, and Scaladoc? If not, delete it.

Scaladoc goes on the public surface of published modules (the
`Z*Container` objects, their `Settings`, the `Docker` facade, the
`Command` ADT) and states the contract: what the layer provides, what
it requires, what it acquires and releases. Private members and test
code carry no doc comments. The Apache license header is required on
every source file (sbt-header enforces it).

## What this codebase does not do

- **Run effects in the middle.** No `unsafeRun` outside app entry
  points and specs.
- **Throw.** No `throw`, no `try/catch` past the boundary
  `ZIO.attempt`. Scalafix `DisableSyntax` rejects `throw`, `null`,
  `return`, `var` in the configured scopes.
- **Use `Future`, `Await`, `ExecutionContext`.** Java/Scala futures
  from libraries are lifted with `ZIO.fromFuture` /
  `ZIO.fromCompletionStage` at the boundary.
- **Use `Option.get`, `.head`, `.asInstanceOf` on data.** The one
  sanctioned cast is the `widen` helper in `InterpreterLive`, which
  exists to recover a dependent type the compiler cannot track; any
  new cast needs the same one-line justification.
- **Use Scala 3-only syntax.** See "Cross-building".
- **Mutable shared state.** `Ref`, not `var`.
- **Stringly-typed APIs.** No raw `String` ids, no `Map[String, Any]`,
  no `ast.Json` beyond the decoder.
- **ZIO 1.x.** Removed as a target (see ARCHITECTURE.md). No
  `zio.console._`, `ZEnv`, `Has`, `ZManaged`, zio-magic.

## Tooling floor

- `sbt fmt` (scalafmt, `.scalafmt.conf` is authoritative,
  `maxColumn = 120`, `runner.dialect = scala3`). Formatting is not a
  discussion.
- `sbt lint` (scalafmt check + scalafix `--check` with the rules in
  `.scalafix.conf`) must pass. `sbt fix` applies scalafix rewrites.
- CI compiles with `-Xfatal-warnings` (`enableStrictCompile` /
  `CI=true`). Warnings are errors; a `@nowarn` without a one-line
  reason is a review flag.
- `Test / fork := true` is mandatory for every module that starts a
  container; without it, containers outlive the test JVM.
- `sbt build` is the local pre-push gate: lint, strict compile, test.
  `sbt build-all` cross-builds every supported Scala version.
