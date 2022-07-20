package io.github.scottweaver.zio.testcontainers.postgres

import zio._
import com.dimafeng.testcontainers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.sql.DriverManager
// import java.sql.Connection
import io.github.scottweaver.models.JdbcInfo
// import javax.sql.DataSource

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

  val live = {

    def makeScopedConnection(container: PostgreSQLContainer) =
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
          .ignoreLogged
      )

    def makeScopedContainer(settings: Settings) =
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
          .ignoreLogged
      )

    ZLayer.scopedEnvironment {
      for {
        settings  <- ZIO.service[Settings]
        container <- makeScopedContainer(settings)
        conn      <- makeScopedConnection(container)
      } yield {
        val jdbcInfo = JdbcInfo(
          driverClassName = container.driverClassName,
          jdbcUrl = container.jdbcUrl,
          username = container.username,
          password = container.password
        )
        val dataSource = new org.postgresql.ds.PGSimpleDataSource()
        dataSource.setUrl(jdbcInfo.jdbcUrl)
        dataSource.setUser(jdbcInfo.username)
        dataSource.setPassword(jdbcInfo.password)

        ZEnvironment(jdbcInfo, conn, dataSource, container)
      }
    }
  }
}
