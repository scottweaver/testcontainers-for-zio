package io.github.scottweaver.zio.testcontainers.mysql

import zio._
import com.dimafeng.testcontainers.MySQLContainer
import org.testcontainers.utility.DockerImageName
import java.sql.DriverManager
import java.sql.Connection

object ZMySQLContainer {

  final case class JdbcInfo(
    driverClassName: String,
    jdbcUrl: String,
    username: String,
    password: String
  )

  type Provides = Has[JdbcInfo] with Has[Connection with AutoCloseable]

  def live(containerVersion: String = "latest"): ZLayer[Any, Nothing, Provides] = {

    def makeManagedConnection(container: MySQLContainer) =
      ZManaged.make(
        ZIO.effect {
          DriverManager.getConnection(
            container.jdbcUrl,
            container.username,
            container.password
          )
        }.orDie
      )(conn =>
        ZIO
          .effect(conn.close())
          .tapError(err => ZIO.effect(println(s"Error closing connection: $err")))
          .ignore
      )

    val makeManagedContainer =
      ZManaged.make(
        ZIO.effect {
          val container = MySQLContainer(mysqlImageVersion = DockerImageName.parse(s"mysql:$containerVersion"))
          container.start()
          container
        }.orDie
      )(container =>
        ZIO
          .effect(container.stop())
          .tapError(err => ZIO.effect(println(s"Error stopping container: $err")))
          .ignore
      )

    ZLayer.fromManagedMany {
      for {
        container <- makeManagedContainer
        conn      <- makeManagedConnection(container)

      } yield {

        val jdbcInfo = JdbcInfo(
          driverClassName = container.driverClassName,
          jdbcUrl = container.jdbcUrl,
          username = container.username,
          password = container.password
        )

        Has(jdbcInfo) ++ Has(conn)
      }

    }
  }

}
