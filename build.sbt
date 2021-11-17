val scala2Version = "2.13.6"
// val zioVersion = "2.0.0-M2"
val zioVersion                 = "1.0.12"
val testcontainersScalaVersion = "0.39.12"
val mysqlConnnectorJVersion    = "8.0.27"

ThisBuild / version := "0.1.0"

lazy val mysql =
  project
    .in(file("modules/mysql"))
    .settings(settings)
    .settings(
      libraryDependencies ++= Seq(
        "com.dimafeng" %% "testcontainers-scala-mysql" % testcontainersScalaVersion,
        "mysql"        % "mysql-connector-java"        % mysqlConnnectorJVersion
      )
    )

lazy val kafka =
  project
    .in(file("modules/kafka"))
    .settings(settings)
    .settings(
      libraryDependencies ++= Seq(
        "com.dimafeng" %% "testcontainers-scala-kafka" % testcontainersScalaVersion

      )
    )

lazy val settings =
  commonSettings ++
    commandAliases

lazy val commonSettings =
  Seq(
    name := "testcontainers-for-zio",
    scalaVersion := scala2Version,
    organization := "io.github.scottweaver",
    libraryDependencies ++= Seq(
      "dev.zio"      %% "zio"                       % zioVersion,
      "com.dimafeng" %% "testcontainers-scala-core" % testcontainersScalaVersion,
      "dev.zio"      %% "zio-test"                  % zioVersion % Test,
      "dev.zio"      %% "zio-test-sbt"              % zioVersion % Test
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )

lazy val commandAliases =
  addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt") ++
    addCommandAlias("check", "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck")

lazy val stdOptions = Seq(
  "-encoding",
  "UTF-8",
  "-explaintypes",
  "-Yrangepos",
  "-feature",
  "-language:higherKinds",
  "-language:existentials",
  "-Xlint:_,-type-parameter-shadow,-byname-implicit",
  "-Xsource:2.13",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-unchecked",
  "-deprecation",
  "-Xfatal-warnings"
)

lazy val stdOpts213 = Seq(
  "-Wunused:imports",
  "-Wvalue-discard",
  "-Wunused:patvars",
  "-Wunused:privates",
  "-Wunused:params"
)

scalacOptions := stdOptions ++ stdOpts213
