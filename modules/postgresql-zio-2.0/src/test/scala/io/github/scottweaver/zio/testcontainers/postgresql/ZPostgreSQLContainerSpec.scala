package io.github.scottweaver.zio.testcontainers.postgres

import zio._
import zio.test._
import zio.test.Assertion._
import java.sql.Connection
import com.dimafeng.testcontainers.PostgreSQLContainer
import io.github.scottweaver.models.JdbcInfo

object ZPostgreSQLContainerSpec extends DefaultRunnableSpec {
  def spec =
    suite("ZPostgreSQLContainerSpec")(
      test("Should start up a Postgres continer that can be queried.") {
        def sqlTestQuery(conn: Connection) =
          ZIO.attempt {
            val stmt = conn.createStatement()
            val rs   = stmt.executeQuery("SELECT 1")
            rs.next()
            rs.getInt(1)
          }

        for {
          conn      <- ZIO.service[Connection]
          container <- ZIO.service[PostgreSQLContainer]
          jdbcInfo  <- ZIO.service[JdbcInfo]
          result    <- sqlTestQuery(conn)
        } yield assert(result)(equalTo(1)) &&
          assert(jdbcInfo.jdbcUrl)(equalTo(container.jdbcUrl)) &&
          assert(jdbcInfo.username)(equalTo(container.username)) &&
          assert(jdbcInfo.password)(equalTo(container.password)) &&
          assert(jdbcInfo.driverClassName)(equalTo(container.driverClassName))
      }.provideLayerShared(
        ZPostgreSQLContainer.Settings.default >>> ZPostgreSQLContainer.live
      )
    )
}
