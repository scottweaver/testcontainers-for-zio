import sbt._
import sbt.Keys._

object SonatypeCredentials extends AutoPlugin {

  override def trigger = allRequirements

  lazy val settings            = {
    val sonatypeUsername    = sys.env.get("SONATYPE_USERNAME")
    val sonatypePassword    = sys.env.get("SONATYPE_PASSWORD")
    val sonatypeCredentials = (for {
      username <- sonatypeUsername
      password <- sonatypePassword
    } yield Credentials(
      "Sonatype Nexus Repository Manager",
      "s01.oss.sonatype.org",
      username,
      password
    )).toSeq

    Seq(
      credentials ++= sonatypeCredentials
    )
  }

  override val projectSettings = settings

}
