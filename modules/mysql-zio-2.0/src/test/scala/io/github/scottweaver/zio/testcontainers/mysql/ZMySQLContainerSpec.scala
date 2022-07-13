package io.github.scottweaver.zio.testcontainers.mysql

import zio._
import zio.test._
import java.sql.Connection
import com.dimafeng.testcontainers.MySQLContainer
import io.github.scottweaver.models.JdbcInfo
import javax.sql.DataSource

object ZMySQLContainerSpec extends ZIOSpecDefault {
  def spec =
    suite("ZMySQLContainerSpec")(
      test("Should start up a MySQL container that can be queried.") {

        def sqlTestQuery(conn: Connection) =
          ZIO.attempt {
            val stmt = conn.createStatement()
            val rs   = stmt.executeQuery("SELECT 1")
            rs.next()
            rs.getInt(1)
          }

        def sqlTestOnDs(ds: DataSource) =
          for {
            conn   <- ZIO.attempt(ds.getConnection)
            result <- sqlTestQuery(conn)

          } yield (result)

        val testCase = for {
          conn      <- ZIO.service[Connection]
          ds        <- ZIO.service[DataSource]
          container <- ZIO.service[MySQLContainer]
          jdbcInfo  <- ZIO.service[JdbcInfo]
          result    <- sqlTestQuery(conn)
          result2   <- sqlTestOnDs(ds)
        } yield assertTrue(
          result == 1,
          result2 == 1,
          jdbcInfo.jdbcUrl == container.jdbcUrl,
          jdbcInfo.username == container.username,
          jdbcInfo.password == container.password,
          jdbcInfo.driverClassName == container.driverClassName
        )

        testCase
      }
    ).provideShared(
      ZMySQLContainer.Settings.default,
      ZMySQLContainer.live
    )
}
