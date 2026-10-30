object V {
  val cassandraDriverVersion     = "4.14.1"
  val scala213Version            = "2.13.8"
  val scala212Version            = "2.12.16"
  val scala3Version              = "3.2.2"
  val supportedScalaVersions     = List(scala213Version, scala212Version, scala3Version)
  val zio1xVersion               = "1.0.15"
  val zio2xVersion               = "2.0.13"
  val zioJsonVersion             = "0.4.2"
  val zioPreludeVersion          = "1.0.0-RC15"
  val zioMagicVersion            = "0.3.10"
  val zioKafkaVersion            = "0.17.1"
  val zio2KafkaVersion           = "2.3.1"
  val zioHttpVersion             = "0.0.4"
  val testcontainersScalaVersion = "0.40.10"
  val mysqlConnnectorJVersion    = "8.0.29"
  val postgresqlDriverVersion    = "42.3.3"
  val slf4jVersion               = "1.7.32"
  val logbackVersion             = "1.2.6"
  val flywayVersion              = "8.1.0"
  val cassandraMigrationsVersion = "2.5.0_v4"
  val liquibaseVersion           = "4.13.0"
  val nettyVersion               = "4.1.79.Final"

  private val versions: Map[String, String] = {
    import org.snakeyaml.engine.v2.api.{Load, LoadSettings}

    import java.util.{List => JList, Map => JMap}
    import scala.jdk.CollectionConverters._

    val doc = new Load(LoadSettings.builder().build())
      .loadFromReader(scala.io.Source.fromFile(".github/workflows/ci.yml").bufferedReader())
    val yaml = doc.asInstanceOf[JMap[String, JMap[String, JMap[String, JMap[String, JMap[String, JList[String]]]]]]]
    val list = yaml.get("jobs").get("test").get("strategy").get("matrix").get("scala").asScala
    list.map(v => (v.split('.').take(2).mkString("."), v)).toMap
  }

  val Scala212: String = versions.getOrElse("2.12", "2.12.16")
  val Scala213: String = versions.getOrElse("2.13", "2.13.8")
  val Scala3: String   = versions.getOrElse("3.2", "3.2.2")
}
