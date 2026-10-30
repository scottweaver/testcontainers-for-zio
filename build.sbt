import ZioEcosystemProjectPlugin.autoImport._

ThisBuild / version       := "0.10.0"
ThisBuild / versionScheme := Some("early-semver")
ThisBuild / organization  := "io.github.scottweaver"
ThisBuild / description   := "Provides ZIO ZLayer wrappers around Scala Testcontainers"
ThisBuild / homepage      := Some(url("https://github.com/scottweaver/testcontainers-for-zio"))
ThisBuild / startYear     := Some(2021)
ThisBuild / licenses      := List("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0.txt"))
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
    `db-migration-aspect-Zio2`,
    liquibaseAspect,
    mysqlZio2,
    postgresZio2,
    kafkaZio2,
    cassandraZio2,
    `cassandra-migration-aspect-Zio2`,
    solrZio2
    // docs
  )

lazy val core = project
  .in(file("modules/core"))
  .settings(
    name := "zillen",
    testSettings,
    publishSettings,
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio-prelude"                   % V.zioPreludeVersion,
      "dev.zio" %% "zio-json"                      % V.zioJsonVersion,
      "dev.zio" %% "zio-streams"                   % V.zioVersion,
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

lazy val `db-migration-aspect-Zio2` = project
  .in(file("modules/db-migration-aspect-zio-2.0"))
  .settings(
    testSettings,
    publishSettings,
    name := "zio-2.0-db-migration-aspect",
    libraryDependencies ++= Seq(
      "org.flywaydb" % "flyway-core" % V.flywayVersion,
      "dev.zio"     %% "zio-test"    % V.zioVersion
    )
  )
  .dependsOn(models, mysqlZio2 % "test->test")

lazy val liquibaseAspect = project
  .in(file("modules/zio-2.0-liquibase-aspect"))
  .settings(
    testSettings,
    publishSettings,
    name := "zio-2.0-liquibase-aspect",
    libraryDependencies ++= Seq(
      "org.liquibase" % "liquibase-core" % V.liquibaseVersion,
      "dev.zio"      %% "zio-test"       % V.zioVersion
    )
  )
  .dependsOn(models, postgresZio2 % "test->test")

lazy val mysqlZio2 =
  project
    .in(file("modules/mysql-zio-2.0"))
    .settings(
      testcontainersScalaSettings,
      name := "zio-2.0-testcontainers-mysql",
      libraryDependencies ++= Seq(
        "com.dimafeng" %% "testcontainers-scala-mysql" % V.testcontainersScalaVersion,
        "mysql"         % "mysql-connector-java"       % V.mysqlConnnectorJVersion
      )
    )
    .dependsOn(models)

lazy val postgresZio2 =
  project
    .in(file("modules/postgresql-zio-2.0"))
    .settings(
      testcontainersScalaSettings,
      name := "zio-2.0-testcontainers-postgresql",
      libraryDependencies ++= Seq(
        "com.dimafeng"  %% "testcontainers-scala-postgresql" % V.testcontainersScalaVersion,
        "org.postgresql" % "postgresql"                      % V.postgresqlDriverVersion
      )
    )
    .dependsOn(models, core)

lazy val kafkaZio2 =
  project
    .in(file("modules/kafka-zio-2.0"))
    .settings(
      testcontainersScalaSettings,
      name := "zio-2.0-testcontainers-kafka",
      libraryDependencies ++= Seq(
        "dev.zio"      %% "zio-kafka"                  % V.zioKafkaVersion,
        "com.dimafeng" %% "testcontainers-scala-kafka" % V.testcontainersScalaVersion
      )
    )

lazy val `cassandra-migration-aspect-Zio2` = project
  .in(file("modules/cassandra-migration-aspect-zio-2.0"))
  .settings(
    testSettings,
    publishSettings,
    name := "zio-2.0-cassandra-migration-aspect",
    libraryDependencies ++= Seq(
      "org.cognitor.cassandra" % "cassandra-migration" % V.cassandraMigrationsVersion,
      "com.datastax.oss"       % "java-driver-core"    % V.cassandraDriverVersion,
      "dev.zio"               %% "zio-test"            % V.zioVersion
    )
  )
  .dependsOn(cassandraZio2 % "test->test")

lazy val cassandraZio2 =
  project
    .in(file("modules/cassandra-zio-2.0"))
    .settings(
      testcontainersScalaSettings,
      name := "zio-2.0-testcontainers-cassandra",
      libraryDependencies ++= Seq(
        "com.dimafeng"    %% "testcontainers-scala-cassandra" % V.testcontainersScalaVersion,
        "com.datastax.oss" % "java-driver-core"               % V.cassandraDriverVersion
      )
    )

lazy val solrZio2 =
  project
    .in(file("modules/solr-zio-2.0"))
    .settings(
      testcontainersScalaSettings,
      name := "zio-2.0-testcontainers-solr",
      libraryDependencies ++= Seq(
        "com.dimafeng" %% "testcontainers-scala-solr" % V.testcontainersScalaVersion,
        "dev.zio"      %% "zio-http"                  % V.zioHttpVersion % Test
      )
    )

def testSettings: Seq[Setting[_]] = Seq(
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
  .in(file("docs-project"))
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
  .enablePlugins(DocusaurusPlugin, ScalaUnidocPlugin, DocusaurusExtrasPlugin)
