package io.github.scottweaver.zio.testcontainers.postgres

import zio._
import zio.test._
import java.sql.Connection
import io.github.scottweaver.models.JdbcInfo
import javax.sql.DataSource
import io.github.scottweaver.zillen.Docker
import io.github.scottweaver.zillen.models._
import io.github.scottweaver.zio.testcontainers.postgresql.PostgresContainer

object PostgresContainerSpec extends ZIOSpecDefault {
  def spec =
    suite("PostgresContainerSpec")(
      test("Should start up a Postgres container that can be queried.") {
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

        for {
          conn      <- ZIO.service[Connection]
          ds        <- ZIO.service[DataSource]
          container <- ZIO.service[InspectContainerResponse]
          _  <- ZIO.service[JdbcInfo]
          result    <- sqlTestQuery(conn)
          result2   <- sqlTestOnDs(ds)
        } yield assertTrue(
          result == 1,
          result2 == 1,
          container.hostConfig.portBindings.findExternalHostPort(5432, Protocol.TCP).nonEmpty,
          container.name.head == ContainerName.unsafeMake("/zio-postgres-test-container") // Docker adds a preceding slash to the original container name.
          // jdbcInfo.jdbcUrl == container.jdbcUrl,
          // jdbcInfo.username == container.username,
          // jdbcInfo.password == container.password,
          // jdbcInfo.driverClassName == container.driverClassName
        )
      }.provideShared(
        Scope.default,
        Docker.layer(),
        PostgresContainer.Settings.default(),
        PostgresContainer.layer
      ) @@ TestAspect.withLiveClock
    )
}
