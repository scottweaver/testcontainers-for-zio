import ZioEcosystemProjectPlugin.autoImport._

ThisBuild / version       := "0.9.0"
ThisBuild / versionScheme := Some("early-semver")
ThisBuild / organization  := "io.github.scottweaver"
ThisBuild / description   := "Provides ZIO ZLayer wrappers around Scala Testcontainers"
ThisBuild / homepage      := Some(url("https://github.com/scottweaver/testcontainers-for-zio"))
ThisBuild / startYear     := Some(2021)
ThisBuild / licenses      := List("Apache-2.0" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt"))
ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/scottweaver/testcontainers-for-zio"),
    "scm:git@github.com:scottweaver/testcontainers-for-zio.git"
  )
)
ThisBuild / developers := List(
  Developer(
    id = "scottweaver",
    name = "Scott T Weaver",
    email = "scott.t.weaver@gmail.com",
    url = url("https://scottweaver.github.io/")
  )
)

lazy val root = project
  .in(file("."))
  .settings(
    name               := "testcontainers-for-zio",
    publish / skip     := true,
    crossScalaVersions := Nil
  )
  .aggregate(
    core,
    models,
    `db-migration-aspect`,
    `db-migration-aspect-Zio2`,
    liquibaseAspect,
    mysql,
    mysqlZio2,
    postgres,
    postgresZio2,
    kafka,
    kafkaZio2,
    cassandra,
    cassandraZio2,
    `cassandra-migration-aspect`,
    `cassandra-migration-aspect-Zio2`
    // benchmarks,
    // docs
  )

lazy val core = project
  .in(file("modules/core"))
  .settings(
    name      := "zillen",
    zioSeries := ZIOSeries.Series2X,
    testSettings,
    publishSettings,
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio-prelude"                   % V.zioPreludeVersion,
      "dev.zio" %% "zio-json"                      % V.zioJsonVersion,
      "dev.zio" %% "zio-streams"                   % V.zio2xVersion,
      "io.netty" % "netty-codec-http"              % V.nettyVersion,
      "io.netty" % "netty-handler"                 % V.nettyVersion,
      "io.netty" % "netty-transport-native-epoll"  % V.nettyVersion classifier "linux-x86_64",
      "io.netty" % "netty-transport-native-kqueue" % V.nettyVersion classifier "osx-x86_64",
      "io.netty" % "netty-transport-native-kqueue" % V.nettyVersion classifier "osx-aarch_64"
    )
  )

lazy val models = project
  .in(file("modules/models"))
  .settings(
    name     := "zio-testcontainers-models",
    needsZio := false,
    publishSettings
  )

lazy val `db-migration-aspect` = project
  .in(file("modules/db-migration-aspect"))
  .settings(
    zioSeries := ZIOSeries.Series1X,
    testSettings,
    publishSettings,
    name := "zio-db-migration-aspect",
    libraryDependencies ++= Seq(
      "org.flywaydb" % "flyway-core" % V.flywayVersion,
      "dev.zio"     %% "zio-test"    % V.zio1xVersion
    )
  )
  .dependsOn(models, mysql % "test->test")

lazy val `db-migration-aspect-Zio2` = project
  .in(file("modules/db-migration-aspect-zio-2.0"))
  .settings(
    zioSeries := ZIOSeries.Series2X,
    testSettings,
    publishSettings,
    name := "zio-2.0-db-migration-aspect",
    libraryDependencies ++= Seq(
      "org.flywaydb" % "flyway-core" % V.flywayVersion,
      "dev.zio"     %% "zio-test"    % V.zio2xVersion
    )
  )
  .dependsOn(models, mysqlZio2 % "test->test")

lazy val liquibaseAspect = project
  .in(file("modules/zio-2.0-liquibase-aspect"))
  .settings(
    zioSeries := ZIOSeries.Series2X,
    testSettings,
    publishSettings,
    name := "zio-2.0-liquibase-aspect",
    libraryDependencies ++= Seq(
      "org.liquibase" % "liquibase-core" % V.liquibaseVersion,
      "dev.zio"      %% "zio-test"       % V.zio2xVersion
    )
  )
  .dependsOn(models, postgresZio2 % "test->test")

lazy val mysql =
  project
    .in(file("modules/mysql"))
    .settings(
      zioSeries := ZIOSeries.Series1X,
      testcontainersScalaSettings,
      name := "zio-testcontainers-mysql",
      libraryDependencies ++= Seq(
        "com.dimafeng" %% "testcontainers-scala-mysql" % V.testcontainersScalaVersion,
        "mysql"         % "mysql-connector-java"       % V.mysqlConnnectorJVersion
      )
    )
    .dependsOn(models)

lazy val mysqlZio2 =
  project
    .in(file("modules/mysql-zio-2.0"))
    .settings(
      zioSeries := ZIOSeries.Series2X,
      testcontainersScalaSettings,
      name := "zio-2.0-testcontainers-mysql",
      libraryDependencies ++= Seq(
        "com.dimafeng" %% "testcontainers-scala-mysql" % V.testcontainersScalaVersion,
        "mysql"         % "mysql-connector-java"       % V.mysqlConnnectorJVersion
      )
    )
    .dependsOn(models)

lazy val postgres =
  project
    .in(file("modules/postgresql"))
    .settings(
      zioSeries := ZIOSeries.Series1X,
      testcontainersScalaSettings,
      name := "zio-testcontainers-postgresql",
      libraryDependencies ++= Seq(
        "com.dimafeng"  %% "testcontainers-scala-postgresql" % V.testcontainersScalaVersion,
        "org.postgresql" % "postgresql"                      % V.postgresqlDriverVersion
      )
    )
    .dependsOn(models)

// addCompilerPlugin("org.scalameta" % "semanticdb-scalac" % "4.5.11" cross CrossVersion.full)
lazy val postgresZio2 =
  project
    .in(file("modules/postgresql-zio-2.0"))
    .settings(
      zioSeries := ZIOSeries.Series2X,
      testcontainersScalaSettings,
      name := "zio-2.0-testcontainers-postgresql",
      libraryDependencies ++= Seq(
        "com.dimafeng"  %% "testcontainers-scala-postgresql" % V.testcontainersScalaVersion,
        "org.postgresql" % "postgresql"                      % V.postgresqlDriverVersion
      )
    )
    .dependsOn(models, core)

lazy val kafka =
  project
    .in(file("modules/kafka"))
    .settings(
      zioSeries := ZIOSeries.Series1X,
      testcontainersScalaSettings,
      name := "zio-testcontainers-kafka",
      libraryDependencies ++= Seq(
        "dev.zio"      %% "zio-kafka"                  % V.zioKafkaVersion,
        "com.dimafeng" %% "testcontainers-scala-kafka" % V.testcontainersScalaVersion
      )
    )

lazy val kafkaZio2 =
  project
    .in(file("modules/kafka-zio-2.0"))
    .settings(
      zioSeries := ZIOSeries.Series2X,
      testcontainersScalaSettings,
      name := "zio-2.0-testcontainers-kafka",
      libraryDependencies ++= Seq(
        "dev.zio"      %% "zio-kafka"                  % V.zio2KafkaVersion,
        "com.dimafeng" %% "testcontainers-scala-kafka" % V.testcontainersScalaVersion
      )
    )

lazy val `cassandra-migration-aspect` = project
  .in(file("modules/cassandra-migration-aspect"))
  .settings(
    zioSeries := ZIOSeries.Series1X,
    testSettings,
    publishSettings,
    name := "zio-cassandra-migration-aspect",
    libraryDependencies ++= Seq(
      "org.cognitor.cassandra" % "cassandra-migration" % V.cassandraMigrationsVersion,
      "com.datastax.oss"       % "java-driver-core"    % V.cassandraDriverVersion,
      "dev.zio"               %% "zio-test"            % V.zio1xVersion
    )
  )
  .dependsOn(cassandra % "test->test")

lazy val `cassandra-migration-aspect-Zio2` = project
  .in(file("modules/cassandra-migration-aspect-zio-2.0"))
  .settings(
    zioSeries := ZIOSeries.Series2X,
    testSettings,
    publishSettings,
    name := "zio-2.0-cassandra-migration-aspect",
    libraryDependencies ++= Seq(
      "org.cognitor.cassandra" % "cassandra-migration" % V.cassandraMigrationsVersion,
      "com.datastax.oss"       % "java-driver-core"    % V.cassandraDriverVersion,
      "dev.zio"               %% "zio-test"            % V.zio1xVersion
    )
  )
  .dependsOn(cassandraZio2 % "test->test")

lazy val cassandra =
  project
    .in(file("modules/cassandra"))
    .settings(
      zioSeries := ZIOSeries.Series1X,
      name      := "zio-testcontainers-cassandra",
      testcontainersScalaSettings,
      libraryDependencies ++= Seq(
        "com.dimafeng"    %% "testcontainers-scala-cassandra" % V.testcontainersScalaVersion,
        "com.datastax.oss" % "java-driver-core"               % V.cassandraDriverVersion
      )
    )

lazy val cassandraZio2 =
  project
    .in(file("modules/cassandra-zio-2.0"))
    .settings(
      zioSeries := ZIOSeries.Series2X,
      testcontainersScalaSettings,
      name := "zio-2.0-testcontainers-cassandra",
      libraryDependencies ++= Seq(
        "com.dimafeng"    %% "testcontainers-scala-cassandra" % V.testcontainersScalaVersion,
        "com.datastax.oss" % "java-driver-core"               % V.cassandraDriverVersion
      )
    )

def testSettings: Seq[Setting[_]] = Seq(
  libraryDependencies ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((3, _)) => Seq()
      case _ if zioSeries.value == ZIOSeries.Series1X && needsZio.value =>
        Seq(
          "io.github.kitlangton" %% "zio-magic" % V.zioMagicVersion % Test
        )
      case _ => Seq()
    }
  },
  Test / fork := true
)

def testcontainersScalaSettings =
  testSettings ++ Seq(
    // Prevent slf4j 2.x from ruining EVERYTHING :(
    dependencyOverrides ++= Seq(
      "org.slf4j" % "slf4j-api" % V.slf4jVersion
    ),
    libraryDependencies ++= Seq(
      "com.dimafeng"  %% "testcontainers-scala-core" % V.testcontainersScalaVersion,
      "org.slf4j"      % "slf4j-api"                 % V.slf4jVersion,
      "ch.qos.logback" % "logback-classic"           % V.logbackVersion % Test
    )
  ) ++ publishSettings

lazy val publishSettings =
  Seq(
    pomIncludeRepository := { _ => false },
    publishTo := {
      val nexus = "https://s01.oss.sonatype.org/"
      if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
      else Some("releases" at nexus + "service/local/staging/deploy/maven2")
    },
    publishMavenStyle := true
  )

lazy val docs = project
  .in(file("docs-source"))
  .settings(
    publish / skip := true,
    moduleName     := "zio-testcontainers-docs",
    scalacOptions -= "-Yno-imports",
    ScalaUnidoc / unidoc / unidocProjectFilter := inProjects(root),
    ScalaUnidoc / unidoc / target              := (LocalRootProject / baseDirectory).value / "website" / "static" / "api",
    cleanFiles += (ScalaUnidoc / unidoc / target).value,
    docusaurusCreateSite     := docusaurusCreateSite.dependsOn(Compile / unidoc).value,
    docusaurusPublishGhpages := docusaurusPublishGhpages.dependsOn(Compile / unidoc).value
  )
  .dependsOn(root)
  .enablePlugins(MdocPlugin, DocusaurusPlugin, ScalaUnidocPlugin)
