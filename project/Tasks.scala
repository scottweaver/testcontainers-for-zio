import sbt._
import sbt.Keys._
import scala.Console
import org.scalafmt.sbt.ScalafmtPlugin.autoImport._
import de.heikoseeberger.sbtheader.HeaderPlugin.autoImport._

object Tasks {
  private val strictCompilationProp = "enable.strict.compilation"

  val enableStrictCompile =
    taskKey[Unit]("Enables stricter compilation e.g. warnings become errors. Compiler Cat is happy 😺!")

  val disableStrictCompile = taskKey[Unit](
    "Disables strict compilation e.g. warnings are no longer treated as errors.  Compiler Cat is aghast at your poor life choices 🙀!"
  )

  private def enableStrictCompileImpl = Def.task {
    val log = streams.value.log
    sys.props.put(strictCompilationProp, "true")
    log.info(Console.GREEN + Console.BOLD + s">>> 😸 Enabled strict compilation 😸 <<<" + Console.RESET)
  }

  private def disableStrictCompilePure(logger: String => Unit) = {
    sys.props.put(strictCompilationProp, "false")
    logger(Console.YELLOW + Console.BOLD + ">>> 🙀 Disabled strict compilation 🙀 <<<" + Console.RESET)
  }

  private def disableStrictCompileImpl = Def.task {
    val log = streams.value.log
    disableStrictCompilePure(log.info(_))
  }

  val protoLint = taskKey[Unit]("Performs source-code linting for formatting and proper OSS license headers.")

  lazy val protoLintImpl =
    Def
      .sequential(
        Compile / scalafmtSbtCheck,
        scalafmtCheckAll,
        Compile / headerCheckAll
      )

  val build = taskKey[Unit]("Prepares sources, compiles and runs tests.")

  private def buildImpl =
    Def
      .sequential(
        protoLint,
        clean,
        enableStrictCompile,
        Test / test
      )
      .andFinally(disableStrictCompilePure(s => println(s"[info] $s")))

  lazy val settings: Seq[Setting[_]] = Seq(
    build                := buildImpl.value,
    enableStrictCompile  := enableStrictCompileImpl.value,
    disableStrictCompile := disableStrictCompileImpl.value,
    protoLint            := protoLintImpl.value
  )

}
