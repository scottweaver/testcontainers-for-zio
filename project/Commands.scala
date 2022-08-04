import sbt._

object Commands {

  lazy val settings =
    addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt") ++
    addCommandAlias("fix", "all scalafix test:scalafix") ++
      addCommandAlias("prepare", "scalafmtSbt; scalafmt; test:scalafmt; headerCreateAll; scalafix; test:scalafix") ++
      addCommandAlias("check", "all lint") ++
      addCommandAlias(
        "publishAll",
        "project /; +publishSigned"
      )
}
