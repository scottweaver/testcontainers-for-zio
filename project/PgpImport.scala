import sbt._
import sbt.Keys._
import scala.sys.process._

object PgpImport extends AutoPlugin {
  override def trigger = allRequirements

  object autoImport {
    val importPgpKey = taskKey[Unit]("Import the pgp key")
  }

  def importPgpfromEnv(): Int = {
    val secret = sys.env("PGP_SECRET")
    val cmd    = (s"echo $secret" #| "base64 --decode" #| s"gpg --batch --import").!
    cmd

  }

  import autoImport._

  lazy val settings            = Seq(
    importPgpKey := {
      val log = streams.value.log
      log.info("Importing PGP key...")
      importPgpfromEnv()
      log.info("PGP key imported successfully!")
    }
  )

  override val projectSettings = settings

}
