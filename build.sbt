ThisBuild / version       := "0.8.0"
ThisBuild / versionScheme := Some("early-semver")
ThisBuild / organization  := "io.github.scottweaver"
ThisBuild / description   := "Provides ZIO ZLayer wrappers around Scala Testcontainers"
ThisBuild / homepage      := Some(url("https://github.com/scottweaver/testcontainers-for-zio"))
ThisBuild / licenses      := List("Apache 2" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt"))
ThisBuild / scmInfo       := Some(
  ScmInfo(
    url("https://github.com/scottweaver/testcontainers-for-zio"),
    "scm:git@github.com:scottweaver/testcontainers-for-zio.git"
  )
)
ThisBuild / developers    := List(
  Developer(
    id = "scottweaver",
    name = "Scott T Weaver",
    email = "scott.t.weaver@gmail.com",
    url = url("https://scottweaver.github.io/")
  )
)

crossScalaVersions := Nil
commandAliases

lazy val models                            = project
  .in(file("modules/models"))
  .settings(settings())
  .settings(name := "zio-testcontainers-models")

lazy val `db-migration-aspect`             = project
  .in(file("modules/db-migration-aspect"))
  .settings(settings())
  .settings(
    name := "zio-db-migration-aspect",
    libraryDependencies ++= Seq(
      "org.flywaydb" % "flyway-core" % V.flywayVersion,
      "dev.zio"     %% "zio-test"    % V.zioVersion
    )
  )
  .dependsOn(models, mysql % "test->test")

lazy val `db-migration-aspect-Zio2`        = project
  .in(file("modules/db-migration-aspect-zio-2.0"))
  .settings(settings(V.zio2Version))
  .settings(
    name := "zio-2.0-db-migration-aspect",
    libraryDependencies ++= Seq(
      "org.flywaydb" % "flyway-core" % V.flywayVersion,
      "dev.zio"     %% "zio-test"    % V.zio2Version
    )
  )
  .dependsOn(models, mysqlZio2 % "test->test")

lazy val liquibaseAspect                   = project
  .in(file("modules/zio-2.0-liquibase-aspect"))
  .settings(settings(V.zio2Version))
  .settings(
    name := "zio-2.0-liquibase-aspect",
    libraryDependencies ++= Seq(
      "org.liquibase" % "liquibase-core" % V.liquibaseVersion,
      "dev.zio"      %% "zio-test"       % V.zio2Version
    )
  )
  .dependsOn(models, postgresZio2 % "test->test")

lazy val mysql                             =
  project
    .in(file("modules/mysql"))
    .settings(settings())
    .settings(
      name := "zio-testcontainers-mysql",
      libraryDependencies ++= Seq(
        "com.dimafeng" %% "testcontainers-scala-mysql" % V.testcontainersScalaVersion,
        "mysql"         % "mysql-connector-java"       % V.mysqlConnnectorJVersion
      )
    )
    .dependsOn(models)

lazy val mysqlZio2                         =
  project
    .in(file("modules/mysql-zio-2.0"))
    .settings(settings(V.zio2Version))
    .settings(
      name := "zio-2.0-testcontainers-mysql",
      libraryDependencies ++= Seq(
        "com.dimafeng" %% "testcontainers-scala-mysql" % V.testcontainersScalaVersion,
        "mysql"         % "mysql-connector-java"       % V.mysqlConnnectorJVersion
      )
    )
    .dependsOn(models)

lazy val postgres                          =
  project
    .in(file("modules/postgresql"))
    .settings(settings())
    .settings(
      name := "zio-testcontainers-postgresql",
      libraryDependencies ++= Seq(
        "com.dimafeng"  %% "testcontainers-scala-postgresql" % V.testcontainersScalaVersion,
        "org.postgresql" % "postgresql"                      % V.postgresqlDriverVersion
      )
    )
    .dependsOn(models)

lazy val postgresZio2                      =
  project
    .in(file("modules/postgresql-zio-2.0"))
    .settings(settings(V.zio2Version))
    .settings(
      name := "zio-2.0-testcontainers-postgresql",
      libraryDependencies ++= Seq(
        "com.dimafeng"  %% "testcontainers-scala-postgresql" % V.testcontainersScalaVersion,
        "org.postgresql" % "postgresql"                      % V.postgresqlDriverVersion
      )
    )
    .dependsOn(models)

lazy val kafka                             =
  project
    .in(file("modules/kafka"))
    .settings(settings())
    .settings(
      name := "zio-testcontainers-kafka",
      libraryDependencies ++= Seq(
        "dev.zio"      %% "zio-kafka"                  % V.zioKafkaVersion,
        "com.dimafeng" %% "testcontainers-scala-kafka" % V.testcontainersScalaVersion
      )
    )

lazy val kafkaZio2                         =
  project
    .in(file("modules/kafka-zio-2.0"))
    .settings(settings(V.zio2Version))
    .settings(
      name := "zio-2.0-testcontainers-kafka",
      libraryDependencies ++= Seq(
        "dev.zio"      %% "zio-kafka"                  % V.zio2KafkaVersion,
        "com.dimafeng" %% "testcontainers-scala-kafka" % V.testcontainersScalaVersion
      )
    )

lazy val `cassandra-migration-aspect`      = project
  .in(file("modules/cassandra-migration-aspect"))
  .settings(settings())
  .settings(
    name := "zio-cassandra-migration-aspect",
    libraryDependencies ++= Seq(
      "org.cognitor.cassandra" % "cassandra-migration" % V.cassandraMigrationsVersion,
      "com.datastax.oss"       % "java-driver-core"    % V.cassandraDriverVersion,
      "dev.zio"               %% "zio-test"            % V.zioVersion
    )
  )
  .dependsOn(cassandra % "test->test")

lazy val `cassandra-migration-aspect-Zio2` = project
  .in(file("modules/cassandra-migration-aspect-zio-2.0"))
  .settings(settings(V.zio2Version))
  .settings(
    name := "zio-2.0-cassandra-migration-aspect",
    libraryDependencies ++= Seq(
      "org.cognitor.cassandra" % "cassandra-migration" % V.cassandraMigrationsVersion,
      "com.datastax.oss"       % "java-driver-core"    % V.cassandraDriverVersion,
      "dev.zio"               %% "zio-test"            % V.zioVersion
    )
  )
  .dependsOn(cassandraZio2 % "test->test")

lazy val cassandra                         =
  project
    .in(file("modules/cassandra"))
    .settings(settings())
    .settings(
      name := "zio-testcontainers-cassandra",
      libraryDependencies ++= Seq(
        "com.dimafeng"    %% "testcontainers-scala-cassandra" % V.testcontainersScalaVersion,
        "com.datastax.oss" % "java-driver-core"               % V.cassandraDriverVersion
      )
    )

lazy val cassandraZio2                     =
  project
    .in(file("modules/cassandra-zio-2.0"))
    .settings(settings(V.zio2Version))
    .settings(
      name := "zio-2.0-testcontainers-cassandra",
      libraryDependencies ++= Seq(
        "com.dimafeng"    %% "testcontainers-scala-cassandra" % V.testcontainersScalaVersion,
        "com.datastax.oss" % "java-driver-core"               % V.cassandraDriverVersion
      )
    )

def settings(zioVersion: String = V.zioVersion) =
  commonSettings(zioVersion) ++
    publishSettings ++
    commandAliases

def commonSettings(zioVersion: String = V.zioVersion) =
  Seq(
    scalaVersion       := V.scala213Version,
    crossScalaVersions := V.supportedScalaVersions,
    scalacOptions      := {
      CrossVersion.partialVersion(scalaVersion.value) match {

        case Some((2, 12)) => stdOpts212
        case Some((2, 13)) => stdOpts213
        case _             => stdOpts3
      }
    },
    // Prevent slf4j 2.x from ruining EVERYTHING :(
    dependencyOverrides ++= Seq(
      "org.slf4j" % "slf4j-api" % V.slf4jVersion
    ),
    libraryDependencies ++= Seq(
      "dev.zio"       %% "zio"                       % zioVersion,
      "com.dimafeng"  %% "testcontainers-scala-core" % V.testcontainersScalaVersion,
      "dev.zio"       %% "zio-test"                  % zioVersion,
      "org.slf4j"      % "slf4j-api"                 % V.slf4jVersion,
      "ch.qos.logback" % "logback-classic"           % V.logbackVersion % Test,
      "dev.zio"       %% "zio-test-sbt"              % zioVersion       % Test
    ),
    libraryDependencies ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((3, _)) => Seq()
        case _            =>
          Seq(
            "io.github.kitlangton" %% "zio-magic" % V.zioMagicVersion % Test
          )
      }
    },
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    Test / fork        := true
  )

lazy val publishSettings                              =
  Seq(
    pomIncludeRepository := { _ => false },
    publishTo            := {
      val nexus = "https://s01.oss.sonatype.org/"
      if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
      else Some("releases" at nexus + "service/local/staging/deploy/maven2")
    },
    publishMavenStyle    := true
  )

lazy val commandAliases                               =
  addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt") ++
    addCommandAlias("check", "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck") ++
    addCommandAlias(
      "publishAll",
      "+cassandra/publishSigned; +cassandraZio2/publishSigned; +models/publishSigned; +mysql/publishSigned; +mysqlZio2/publishSigned; +postgres/publishSigned; +postgresZio2/publishSigned; +kafka/publishSigned; +kafkaZio2/publishSigned; +db-migration-aspect/publishSigned; +db-migration-aspect-Zio2/publishSigned; +liquibaseAspect/publishSigned; +cassandra-migration-aspect/publishSigned; +cassandra-migration-aspect-Zio2/publishSigned"
    )

lazy val stdOpts212                                   = Seq(
  "-encoding",
  "UTF-8",
  "-explaintypes",
  "-Yrangepos",
  "-feature",
  "-language:higherKinds",
  "-language:existentials",
  "-language:postfixOps",
  "-Xlint:_,-type-parameter-shadow",
  "-Xsource:2.13",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-unchecked",
  "-deprecation",
  "-Xfatal-warnings"
)

lazy val stdOpts213                                   = Seq(
  "-Wunused:imports",
  "-Wunused:params",
  "-Wunused:patvars",
  "-Wunused:privates",
  "-Wvalue-discard",
  "-Xfatal-warnings",
  "-Xlint:_,-type-parameter-shadow,-byname-implicit",
  "-Xsource:2.13",
  "-Yrangepos",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-explaintypes",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:postfixOps",
  "-unchecked"
)

lazy val stdOpts3                                     = Seq(
  "-Xfatal-warnings",
  "-deprecation",
  "-explaintypes",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:postfixOps",
  "-unchecked",
  "-encoding",
  "UTF-8",
  "-source:3.0-migration"
)

ThisBuild / scalacOptions := stdOpts213
