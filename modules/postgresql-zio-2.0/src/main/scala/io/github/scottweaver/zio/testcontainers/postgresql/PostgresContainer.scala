package io.github.scottweaver.zio.testcontainers.postgresql

import io.github.scottweaver.zillen._
import io.github.scottweaver.models.JdbcInfo
import zio._
import java.sql._

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

    def default(builder: Settings => Settings = identity) = ZLayer.succeed(
      builder(
        Settings(
          imageVersion = "latest",
          password = "password",
          databaseName = None,
          username = None,
          additionalEnv = Docker.makeEnv()
        )
      )
    )
  }

  def makeUrl(portMap: PortMap, settings: Settings) = {
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

  def makeConnection(url: String) = {
    val acquireConn = ZIO
      .attempt(DriverManager.getConnection(url))
    val releaseConn = (conn: Connection) => ZIO.attempt(conn.close()).ignoreLogged
    ZIO
      .acquireRelease(acquireConn)(releaseConn)
      .tapError(t => ZIO.logError(s"Failed to establish a connection to Postgres @ '$url'. Cause: ${t.getMessage})"))
  }

  def check(conn: Connection) =
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

  val makeCommand: DockerIO[Settings with Network, Command.CreateContainer] = {
    val exposedPorts = Docker.makeExposedTCPPorts(5432)
    for {
      settings <- ZIO.service[Settings]
      name     <- Docker.makeContainerNameZIO("zio-postgres-test-container")
      image    <- settings.toImage
      portMap  <- Docker.automapExposedPorts(exposedPorts)
    } yield Docker.cmd.createContainer(
      env = settings.toEnv,
      exposedPorts = exposedPorts,
      hostConfig = Docker.makeHostConfig(portMap),
      image = image,
      containerName = Some(name)
    )

  }

  val layer = ZLayer.fromZIOEnvironment {
    for {
      settings                 <- ZIO.service[Settings]
      containerAndPromise      <- makeCommand.flatMap(Docker.makeScopedContainer)
      (createResponse, promise) = containerAndPromise
      _                        <- promise.await
      container                <- Docker.inspectContainer(createResponse.id)
      url <- makeUrl(container.hostConfig.portBindings, settings)
      driverClassName <- ZIO
                           .attempt(DriverManager.getDriver(url).getClass.getName)
                           .mapError(Docker.invalidConfig(s"Failed to get driver class name for Postgres @ '$url'."))
      ready    = makeConnection(url).flatMap(check).provide(Scope.default)
      promise <- ReadyCheck.makePromise[Any](createResponse.id, _ => ready)
      _       <- promise.await
      conn    <- makeConnection(url).orDie
    } yield {
      val jdbcInfo   = JdbcInfo(driverClassName, url, settings.username.getOrElse("postgres"), settings.password)
      val dataSource = new org.postgresql.ds.PGSimpleDataSource()
      dataSource.setUrl(jdbcInfo.jdbcUrl)
      dataSource.setUser(jdbcInfo.username)
      dataSource.setPassword(jdbcInfo.password)

      ZEnvironment(jdbcInfo, dataSource, conn, container)
    }

  }

}
