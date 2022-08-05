import sbt._
import sbt.Keys._
import complete.DefaultParsers._
import sys.process._
import mdoc.DocusaurusPlugin.website

object DocusaurusExtrasPlugin extends AutoPlugin {

  override def trigger = allRequirements

  val docusaurus = inputKey[Unit]("docusaurus")

  val docusaurusImpl = Def.inputTask[Unit] {

    val log               = streams.value.log
    val args: Seq[String] = spaceDelimited("<arg>").parsed

    val plogger = ProcessLogger(log.info(_), log.error(_))

    try {
      Process(List("yarn", "install"), cwd = website.value).!(plogger)
      Process(List("yarn", "run") ++ args, cwd = website.value).!(plogger)

    } catch {

      case e: Throwable =>
        log.error(s"Failed to execute docusaurus command.  Cause: ${e.getMessage}")
    } finally {
      log.info(s"Docusaurus finished")
    }

  }

  override def projectSettings: Seq[Setting[_]] = Seq(
    docusaurus := docusaurusImpl.evaluated
  )

}
