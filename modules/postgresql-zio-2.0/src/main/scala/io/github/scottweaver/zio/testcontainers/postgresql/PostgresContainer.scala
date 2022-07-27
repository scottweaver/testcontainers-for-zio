package io.github.scottweaver.zio.testcontainers.postgresql

import io.github.scottweaver.zillen.models._
import io.github.scottweaver.zillen._
import zio._
import java.sql.DriverManager
import java.sql.Connection
import io.github.scottweaver.models.JdbcInfo
import io.github.scottweaver.zillen.DockerContainerFailure._

object PostgresContainer {

  final case class Settings(
    imageVersion: String,
    databaseName: String,
    username: String,
    password: String
  ) {

    private[postgresql] def toEnv: Env =
      Env.make(
        "POSTGRES_USER"     -> username,
        "POSTGRES_PASSWORD" -> password,
        "POSTGRES_DB"       -> databaseName
      )

    private[postgresql] def toImage = Image.make(s"postgres:$imageVersion")
  }

  def makeUrl(portMap: PortMap, settings: Settings) = {
    val hostInterface = portMap.findExternalHostPort(5432, Protocol.TCP)
    hostInterface match {
      case Some(hostInterface) =>
        ZIO.succeed(
          s"jdbc:postgresql://${hostInterface.hostAddress}/${settings.databaseName}?user=${settings.username}&password=${settings.password}"
        )
      case None =>
        ZIO.fail(
          DockerContainerFailure.InvalidDockerConfiguration(
            s"No listening host port found for Postgres container for internal port '5432/tcp'."
          )
        )
    }
  }

  def makeConnection(url: String) = {
    val acquireConn = ZIO
      .attempt(DriverManager.getConnection(url))
      .mapError(fromConfigurationException(s"Failed to establish a connection to Postgres", _))
    val releaseConn = (conn: Connection) => ZIO.attempt(conn.close()).ignoreLogged
    ZIO.acquireRelease(acquireConn)(releaseConn)
  }

  def check(conn: Connection) =
    (ZIO.attemptBlocking {
      val stmt = conn.createStatement()
      val rs   = stmt.executeQuery("SELECT 1")
      rs.next()
      rs.getInt(1)
    }.mapError(
      fromConfigurationException(s"Failed to execute query against Postgres.", _)
    ).flatMap { i =>
      if (i == 1)
        ZIO.succeed(true)
      else
        ZIO.fail(
          DockerContainerFailure.ContainerReadyCheckFailure(s"Postgres query check failed, expected 1, got $i")
        )
    }).catchAll { case _ =>
      ZIO.succeed(false)
    }

  val makeCommand: ZIO[Settings with Network, DockerContainerFailure, Command.CreateContainer] = {
    val exposedPorts = ProtocolPort.Exposed.make(ProtocolPort.makeTCPPort(5432))
    for {
      settings <- ZIO.service[Settings]
      name <- ContainerName
                .make("zio-postgres-test-container")
                .toZIO
                .mapError(DockerContainerFailure.InvalidDockerConfiguration(_))
      image <- settings.toImage.toZIO.mapError(DockerContainerFailure.InvalidDockerConfiguration(_, None))
      portMap <- PortMap
                   .makeFromExposedPorts(exposedPorts)
                   .mapError(
                     fromConfigurationException(
                       s"Failed to create Docker PortMap.",
                       _
                     )
                   )
    } yield Command.CreateContainer(
      env = settings.toEnv,
      exposedPorts = exposedPorts,
      hostConfig = HostConfig(portMap),
      image = image,
      containerName = Some(name)
    )

  }

  val layer = ZLayer.fromZIO {
    for {
      settings                 <- ZIO.service[Settings]
      containerAndPromise      <- makeCommand.flatMap(Container.makeScopedContainer)
      (createResponse, promise) = containerAndPromise
      inspectAndStatusResponse <- promise.await
      (inspectResp, status)     = inspectAndStatusResponse
      url                      <- makeUrl(inspectResp.hostConfig.portBindings, settings)
      driverClassName <- ZIO
                           .attempt(DriverManager.getDriver(url).getClass.getName)
                           .mapError(DockerContainerFailure.fromConfigurationException(_))
      conn <- makeConnection(url)
      ready = check(conn)
      _    <- ReadyCheck.awaitReadyContainer(createResponse.id, _ => ready)
    } yield {
      val jdbcInfo   = JdbcInfo(driverClassName, url, settings.username, settings.password)
      val dataSource = new org.postgresql.ds.PGSimpleDataSource()
      dataSource.setUrl(jdbcInfo.jdbcUrl)
      dataSource.setUser(jdbcInfo.username)
      dataSource.setPassword(jdbcInfo.password)

      ZEnvironment(jdbcInfo, dataSource, conn, inspectResp)
    }

  }

}
