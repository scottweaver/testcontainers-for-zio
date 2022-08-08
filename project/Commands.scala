import sbt._
import Keys._
import scala.collection.immutable.ListMap

object Commands {

  final case class ComposableCommand(commandStrings: List[String], name: String = "", description: String = "") {
    self =>

    private def choose(name1: String, name2: String): String =
      if (name1.isEmpty) name2 else if (name2.isEmpty) name1 else name1

    def before(other: ComposableCommand): ComposableCommand =
      ComposableCommand(
        other.commandStrings ++ commandStrings,
        choose(name, other.name),
        choose(description, other.description)
      )

    def <<:(other: ComposableCommand): ComposableCommand = before(other)

    def <<:(other: String): ComposableCommand = self <<: ComposableCommand(other :: Nil, name, description)

    def andThen(other: ComposableCommand): ComposableCommand =
      ComposableCommand(
        commandStrings ++ other.commandStrings,
        choose(other.name, name),
        choose(other.description, description)
      )

    def >>(other: ComposableCommand): ComposableCommand = andThen(other)

    def >>(other: String): ComposableCommand = self >> ComposableCommand(other :: Nil)

    lazy val toCommand = Command.command(name, description, description)(toState)

    def toState(state: State) = commandStrings ::: state

    def describe(newName: String, newDesc: String): ComposableCommand = copy(name = newName, description = newDesc)

    def ??(newName: String, newDesc: String): ComposableCommand = describe(newName, newDesc)

    val toItem = name -> description

  }

  object ComposableCommand {

    def make(commands: String*) = ComposableCommand(commands.toList)

    val quietOff = make("set welcomeBannerEnabled := true")

    val quietOn = make("set welcomeBannerEnabled := false")
    
    val buildAll = quietOn >>  "project /" >> "+build" >> quietOff ?? ("build-all", s"Builds all modules for all defined Scala cross versions: ${V.Scala212}, ${V.Scala213} and ${V.Scala3}.")

    def setScalaVersion(scalaVersion: String) = make(s"++$scalaVersion")

    def scalafix(scalaVersion: String, args: String = "") =
      setScalaVersion(scalaVersion) >> s"scalafix ${args}".trim() >> s"Test / scalafix ${args}".trim()

    val fix = quietOn >> scalafix(V.Scala213) >> scalafix(
      V.Scala212
    ) >> quietOff ?? ("fix", "Fixes source files using using scalafix")

    val fixLint = quietOn >> scalafix(V.Scala213, "--check") >> scalafix(V.Scala212, "--check") >> quietOff

    val fmt =
      quietOn >> "scalafmtSbt" >> "+scalafmt" >> "+Test / scalafmt" >> quietOff ?? ("fmt", "Formats source files using scalafmt.")

    val lint =
      quietOn >> "enableStrictCompile"  >> "+scalafmtSbtCheck" >> "+scalafmtCheckAll" >> "+headerCheckAll" >> fixLint >> "disableStrictCompile" >> quietOff ?? ("lint", "Verifies that all source files are properly formatted, have the correct license headers and have had all scalafix rules applied.")

    val prepare =
      quietOn >> "+headerCreateAll" >> "+scalafmtSbt" >> "+scalafmt" >> "+Test / scalafmt" >> fix >> quietOff ?? ("prepare", "Prepares sources by applying scalafmt, adding missing license headers and running scalafix.")

    val publishAll =
      quietOn >> "project /" >> "+publishSigned" >> quietOff ?? ("publish-all", "Signs and publishes all artifacts to Maven Central.")

    val site =
      quietOn >> "docs/clean" >> "docs/docusaurusCreateSite" >> quietOff ?? ("site", "Builds the documentation microsite.")

    val makeHelp = ListMap(
      lint.toItem,
      fmt.toItem,
      fix.toItem,
      prepare.toItem,
      buildAll.toItem,
      site.toItem,
      prepare.toItem,
      "quiet" -> "`quite on` mutes the welcome banner whilst `quiet off` un-mutes it.",
      publishAll.toItem
    )

  }

  import ComposableCommand._

  val quiet = Command.single("quiet") { (state, arg) =>
    val bannerEnabled = AttributeKey[Boolean]("welcomeBannerEnabled")

    arg.trim.toLowerCase() match {
      case "on" | "true" | "1" =>
        println("Welcome banner is off.")
        quietOn.toState(state)
      case "off" | "false" | "0" =>
        println("Welcome banner is on.")
        quietOff.toState(state)
      case invalid =>
        println(s"Invalid 'quiet' argument: ${invalid}")
        state.fail
    }

  }

  lazy val settings: Seq[Setting[_]] = Seq(
    commands ++= Seq(
      fix.toCommand,
      fmt.toCommand,
      quiet,
      lint.toCommand,
      prepare.toCommand,
      buildAll.toCommand,
      publishAll.toCommand,
      site.toCommand
    )
  )
}
