import explicitdeps.ExplicitDepsPlugin.autoImport._
import scalafix.sbt.ScalafixPlugin.autoImport._
import sbt.Keys._
import sbt._
import sbtbuildinfo.BuildInfoKeys._
import sbtbuildinfo._
import scala.collection.mutable.LinkedHashMap
import scala.collection.immutable.ListMap

object ZioEcosystemProjectPlugin extends AutoPlugin {

  override def trigger = allRequirements

  /**
   * How many forked test JVMs may run at once across the build. sbt's default
   * is 1, which serialises every container suite; each module here starts one
   * container, so a handful in flight is what the CI runner can carry.
   */
  val forkedTestGroups: Int =
    sys.env.get("SBT_FORKED_TEST_GROUPS").flatMap(v => scala.util.Try(v.toInt).toOption).getOrElse(4)

  override def globalSettings: Seq[Setting[_]] = Seq(
    concurrentRestrictions := Seq(
      Tags.limitAll(math.max(EvaluateTask.SystemProcessors, forkedTestGroups)),
      Tags.limit(Tags.ForkedTestGroup, forkedTestGroups),
      Tags.exclusiveGroup(Tags.Clean)
    )
  )

  object autoImport {

    val needsZio = settingKey[Boolean]("Indicates whether or not the project needs ZIO libraries.")

    val welcomeBannerEnabled = settingKey[Boolean]("Indicates whether or not to enable the welcome banner.")

    val usefulTasksAndSettings = settingKey[Map[String, String]](
      "A map of useful tasks and settings that will be displayed as part of the welcome banner."
    )

  }

  private def buildInfoSettings(packageName: String) =
    Seq(
      buildInfoKeys    := Seq[BuildInfoKey](organization, moduleName, name, version, scalaVersion, sbtVersion, isSnapshot),
      buildInfoPackage := packageName
    )

  // Keep this consistent with the version in .core-tests/shared/src/test/scala/REPLSpec.scala
  private val replSettings = makeReplSettings {
    """|import zio._
       |import zio.console._
       |import zio.duration._
       |import zio.Runtime.default._
       |implicit class RunSyntax[A](io: ZIO[ZEnv, Any, A]){ def unsafeRun: A = Runtime.default.unsafeRun(io.provideLayer(ZEnv.live)) }
    """.stripMargin
  }

  // Keep this consistent with the version in .streams-tests/shared/src/test/scala/StreamREPLSpec.scala
  private val streamReplSettings = makeReplSettings {
    """|import zio._
       |import zio.console._
       |import zio.duration._
       |import zio.stream._
       |import zio.Runtime.default._
       |implicit class RunSyntax[A](io: ZIO[ZEnv, Any, A]){ def unsafeRun: A = Runtime.default.unsafeRun(io.provideLayer(ZEnv.live)) }
    """.stripMargin
  }

  private def makeReplSettings(initialCommandsStr: String) = Seq(
    // In the repl most warnings are useless or worse.
    // This is intentionally := as it's more direct to enumerate the few
    // options we do want than to try to subtract off the ones we don't.
    // One of -Ydelambdafy:inline or -Yrepl-class-based must be given to
    // avoid deadlocking on parallel operations, see
    //   https://issues.scala-lang.org/browse/SI-9076
    Compile / console / scalacOptions := Seq(
      "-Ypartial-unification",
      "-language:higherKinds",
      "-language:existentials",
      "-Yno-adapted-args",
      "-Xsource:2.13",
      "-Yrepl-class-based"
    ),
    Compile / console / initialCommands := initialCommandsStr
  )

  import autoImport._

  private val defaultTasksAndSettings: Map[String, String] = Commands.ComposableCommand.makeHelp ++ ListMap(
    "build"                                       -> "Lints source files then strictly compiles and runs tests.",
    "enableStrictCompile"                         -> "Enables strict compilation e.g. warnings become errors.",
    "disableStrictCompile"                        -> "Disables strict compilation e.g. warnings are no longer treated as errors.",
    "~compile"                                    -> "Compiles all modules (file-watch enabled)",
    "test"                                        -> "Runs all tests",
    """testOnly *.YourSpec -- -t \"YourLabel\"""" -> "Only runs tests with matching term e.g."
  )

  def stdSettings: Seq[Setting[_]] = Seq(
    crossScalaVersions     := Seq(V.Scala213, V.Scala3),
    scalaVersion           := V.Scala213,
    needsZio               := true,
    welcomeBannerEnabled   := true,
    usefulTasksAndSettings := defaultTasksAndSettings,
    scalacOptions          := ScalaCompilerSettings.stdScalacOptions(scalaVersion.value),
    libraryDependencies ++= {
      if (needsZio.value)
        Seq(
          "dev.zio" %% "zio"          % V.zioVersion,
          "dev.zio" %% "zio-test"     % V.zioVersion,
          "dev.zio" %% "zio-test-sbt" % V.zioVersion % Test
        )
      else Seq.empty
    },
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    semanticdbEnabled := CrossVersion
      .partialVersion(scalaVersion.value)
      .exists(_._1 == 2), // scalafix needs it on 2.13 only
    semanticdbOptions += "-P:semanticdb:synthetics:on",
    semanticdbVersion := scalafixSemanticdb.revision, // use Scalafix compatible version
    ThisBuild / scalafixDependencies ++= List(
      "com.github.vovapolu" %% "scaluzzi" % "0.1.23"
    ),
    Test / parallelExecution := true,
    incOptions ~= (_.withLogRecompileOnMacro(false)),
    autoAPIMappings := true
  )

  def welcomeMessage = onLoadMessage := {
    if (welcomeBannerEnabled.value) {
      import scala.Console

      val maxLen = usefulTasksAndSettings.value.keys.map(_.length).max

      def normalizedPadding(s: String) = " " * (maxLen - s.length)

      def header(text: String): String = s"${Console.RED}$text${Console.RESET}"

      def item(text: String): String    = s"${Console.GREEN}> ${Console.CYAN}$text${Console.RESET}"
      def subItem(text: String): String = s"  ${Console.YELLOW}> ${Console.CYAN}$text${Console.RESET}"

      s"""|${Banner.trueColor(s"🐳 ${name.value} v.${version.value}")}
          |Useful sbt tasks:
          |${usefulTasksAndSettings.value.map { case (task, description) =>
        s"${item(task)} ${normalizedPadding(task)}${description}"
      }
        .mkString("\n")}
      """.stripMargin

    } else ""
  }

  override def projectSettings: Seq[Setting[_]] = stdSettings ++ Tasks.settings ++ Commands.settings ++ welcomeMessage
}
