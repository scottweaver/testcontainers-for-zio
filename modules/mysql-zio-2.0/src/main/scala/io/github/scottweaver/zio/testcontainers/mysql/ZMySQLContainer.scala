package io.github.scottweaver.zio.testcontainers.mysql

import zio._
import com.dimafeng.testcontainers.MySQLContainer
import org.testcontainers.utility.DockerImageName
import java.sql.DriverManager
import io.github.scottweaver.models.JdbcInfo
import com.mysql.cj.jdbc.MysqlDataSource
object ZMySQLContainer {

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
        MySQLContainer.defaultDatabaseName,
        MySQLContainer.defaultUsername,
        MySQLContainer.defaultPassword
      )
    )
  }

  val live = {

    def makeScopedConnection(container: MySQLContainer) =
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
          val containerDef = MySQLContainer.Def(
            dockerImageName = DockerImageName.parse(s"mysql:${settings.imageVersion}"),
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

        val dataSource = new MysqlDataSource()
        dataSource.setUrl(container.jdbcUrl)
        dataSource.setUser(container.username)
        dataSource.setPassword(container.password)

        ZEnvironment(jdbcInfo, conn, dataSource, container)
      }

    }
  }

}
