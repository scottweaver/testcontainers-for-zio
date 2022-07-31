package io.github.scottweaver.zio.testcontainers.postgresql

import io.github.scottweaver.zillen._
import io.github.scottweaver.models.JdbcInfo
import java.sql._
import javax.sql.DataSource
import zio._

final class PostgresContainer
    extends ContainerBootstrap[PostgresContainer.RIn, PostgresContainer.ROut, PostgresContainer](
      containerName = models.ContainerName("zio-postgres-test-container"),
      exposedPorts = Docker.makeExposedTCPPorts(5432),
      makeImage = ZIO.serviceWithZIO[PostgresContainer.Settings](settings =>
        Docker.makeImageZIO(s"postgres:${settings.imageVersion}")
      ),
      makeEnv = ZIO.serviceWith[PostgresContainer.Settings](_.toEnv)
    ) {

  private def check(conn: Connection) =
    (ZIO.attemptBlocking {
      val stmt = conn.createStatement()
      val rs   = stmt.executeQuery("SELECT 1")
      rs.next()
      rs.getInt(1)
    }.mapError(
      Docker.invalidConfig(s"Failed to execute query against Postgres.")
    ).flatMap { i =>
      if (i == 1)
        ZIO.succeed(true)
      else
        Docker.failReadyCheckFailed(s"Postgres query check failed, expected 1, got $i")
    }).catchAll { case _ =>
      ZIO.succeed(false)
    }

  private def makeUrl(portMap: PortMap, settings: PostgresContainer.Settings) = {
    val hostInterface = portMap.findExternalHostPort(5432, Docker.protocol.TCP)
    val user          = settings.username.getOrElse("postgres")
    val databaseName  = settings.databaseName.getOrElse(user)
    hostInterface match {
      case Some(hostInterface) =>
        ZIO.succeed(
          s"jdbc:postgresql://${hostInterface.hostAddress}/${databaseName}?user=${user}&password=${settings.password}"
        )
      case None =>
        Docker.failInvalidConfig(
          s"No listening host port found for Postgres container for internal port '5432/tcp'."
        )
    }
  }

  private def makeConnection(url: String) = {
    val acquireConn = ZIO
      .attempt(DriverManager.getConnection(url))
    val releaseConn = (conn: Connection) => ZIO.attempt(conn.close()).ignoreLogged
    ZIO
      .acquireRelease(acquireConn)(releaseConn)
      .tapError(t => ZIO.logWarning(s"Failed to establish a connection to Postgres @ '$url'. Cause: ${t.getMessage})"))
  }

  def readyCheck(container: Container): RIO[PostgresContainer.Settings with Scope, Boolean] =
    for {
      settings <- ZIO.service[PostgresContainer.Settings]
      url      <- makeUrl(container.networkSettings.ports, settings).mapError(_.asException)
      conn     <- makeConnection(url)
      result   <- check(conn)
    } yield result

  def makeZEnvironment(
    container: Container
  ): ZIO[PostgresContainer.Settings with Scope, Nothing, ZEnvironment[PostgresContainer.ROut]] =
    (for {
      settings        <- ZIO.service[PostgresContainer.Settings]
      url             <- makeUrl(container.networkSettings.ports, settings).mapError(_.asException)
      conn            <- makeConnection(url)
      driverClassName <- ZIO.attempt(DriverManager.getDriver(url).getClass.getName)
    } yield {
      val jdbcInfo   = JdbcInfo(driverClassName, url, settings.username.getOrElse("postgres"), settings.password)
      val dataSource = new org.postgresql.ds.PGSimpleDataSource()
      dataSource.setUrl(url)
      dataSource.setUser(jdbcInfo.username)
      dataSource.setPassword(jdbcInfo.password)

      ZEnvironment(jdbcInfo, dataSource, conn, container)
    }).orDie

}

object PostgresContainer {
  final case class Settings(
    imageVersion: String,
    password: String,
    databaseName: Option[String],
    username: Option[String],
    additionalEnv: Env
  ) {

    private[postgresql] def toEnv: Env =
      Docker
        .makeEnv(
          "POSTGRES_PASSWORD" -> password
        )
        .withOptionals(
          "POSTGRES_USER" -> username,
          "POSTGRES_DB"   -> databaseName
        ) && additionalEnv

    private[postgresql] def toImage = Docker.makeImageZIO(s"postgres:$imageVersion")
  }

  object Settings {

    def default(
      builder: Settings => Settings = identity,
      containerSettingsBuilder: ContainerSettings[PostgresContainer] => ContainerSettings[PostgresContainer] = identity
    ) = ZLayer.succeed {
      builder(
        Settings(
          imageVersion = "latest",
          password = "password",
          databaseName = None,
          username = None,
          additionalEnv = Docker.makeEnv()
        )
      )
    } ++ ContainerSettings.default[PostgresContainer](containerSettingsBuilder)
  }

  type ROut = JdbcInfo with DataSource with Connection with Container

  type RIn = Settings with Scope

  val layer = (new PostgresContainer).layer

}
