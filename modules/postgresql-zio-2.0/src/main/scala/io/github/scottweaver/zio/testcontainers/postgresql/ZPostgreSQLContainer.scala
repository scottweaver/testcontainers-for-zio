package io.github.scottweaver.zio.testcontainers.postgres

import zio._
import com.dimafeng.testcontainers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.sql.DriverManager
import java.sql.Connection
import io.github.scottweaver.models.JdbcInfo

object ZPostgreSQLContainer {
  final case class Settings(
    imageVersion: String,
    databaseName: String,
    username: String,
    password: String
  )

  object Settings {
    val default = ZLayer.succeed(
      Settings(
        "latest",
        PostgreSQLContainer.defaultDatabaseName,
        PostgreSQLContainer.defaultUsername,
        PostgreSQLContainer.defaultPassword
      )
    )
  }

  // type Provides = JdbcInfo with Connection with AutoCloseable with PostgreSQLContainer
  type Provides = JdbcInfo with Connection with PostgreSQLContainer

  // val live: ZLayer[Settings, Nothing, Provides] = {
  val live = {

    def makeManagedConnection(container: PostgreSQLContainer) =
      ZIO.acquireRelease(
        ZIO.attempt {
          DriverManager.getConnection(
            container.jdbcUrl,
            container.username,
            container.password
          )
        }.orDie
      )(conn =>
        ZIO
          .attempt(conn.close())
          .tapError(err => ZIO.attempt(println(s"Error closing connection: $err")))
          .ignore
      )

    def makeManagedContainer(settings: Settings) =
      ZIO.acquireRelease(
        ZIO.attempt {
          val containerDef = PostgreSQLContainer.Def(
            dockerImageName = DockerImageName.parse(s"postgres:${settings.imageVersion}"),
            databaseName = settings.databaseName,
            username = settings.username,
            password = settings.password
          )
          containerDef.start()
        }.orDie
      )(container =>
        ZIO
          .attempt(container.stop())
          .tapError(err => ZIO.attempt(s"Error stopping container: $err"))
          .ignore
      )

    ZLayer.scopedEnvironment {
      for {
        settings  <- ZIO.service[Settings]
        container <- makeManagedContainer(settings)
        conn      <- makeManagedConnection(container)
      } yield {
        val jdbcInfo = JdbcInfo(
          driverClassName = container.driverClassName,
          jdbcUrl = container.jdbcUrl,
          username = container.username,
          password = container.password
        )

        ZEnvironment(jdbcInfo, conn, container)
      }
    }
  }
}
