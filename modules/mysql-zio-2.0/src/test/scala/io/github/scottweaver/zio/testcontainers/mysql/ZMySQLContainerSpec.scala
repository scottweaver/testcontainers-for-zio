package io.github.scottweaver.zio.testcontainers.mysql

import zio._
import zio.test._
import zio.test.Assertion._
import java.sql.Connection
import com.dimafeng.testcontainers.MySQLContainer
import io.github.scottweaver.models.JdbcInfo

object ZMySQLContainerSpec extends DefaultRunnableSpec {
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

        val testCase = for {
          conn      <- ZIO.service[Connection]
          container <- ZIO.service[MySQLContainer]
          jdbcInfo  <- ZIO.service[JdbcInfo]
          result    <- sqlTestQuery(conn)
        } yield assert(result)(equalTo(1)) &&
        assert(jdbcInfo.jdbcUrl)(equalTo(container.jdbcUrl)) &&
        assert(jdbcInfo.username)(equalTo(container.username)) &&
        assert(jdbcInfo.password)(equalTo(container.password)) &&
        assert(jdbcInfo.driverClassName)(equalTo(container.driverClassName))

        testCase

      }
    ).provideLayerShared(
      ZMySQLContainer.Settings.default >>> ZMySQLContainer.live
    )
}
