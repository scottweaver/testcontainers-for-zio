import sbt._

object Commands {

  lazy val settings =
    addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt") ++
      addCommandAlias("prepare", "all scalafmtSbt scalafmt test:scalafmt headerCreateAll") ++
      addCommandAlias("check", "all protoLint") ++
      addCommandAlias(
        "publishAll",
        "project /; +publishSigned"
      )
}
