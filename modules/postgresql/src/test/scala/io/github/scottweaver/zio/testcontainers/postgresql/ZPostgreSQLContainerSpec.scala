/*
 * Copyright 2021 io.github.scottweaver
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.scottweaver.zio.testcontainers.postgres

import com.dimafeng.testcontainers.PostgreSQLContainer
import io.github.scottweaver.models.JdbcInfo
import zio._
import zio.test._

import java.sql.Connection
import javax.sql.DataSource

object ZPostgreSQLContainerSpec extends DefaultRunnableSpec {
  def spec =
    suite("ZPostgreSQLContainerSpec")(
      testM("Should start up a Postgres continer that can be queried.") {
        def sqlTestQuery(conn: Connection) =
          ZIO.effect {
            val stmt = conn.createStatement()
            val rs   = stmt.executeQuery("SELECT 1")
            rs.next()
            rs.getInt(1)
          }

        def sqlTestOnDs(ds: DataSource) =
          for {
            conn   <- ZIO.effect(ds.getConnection)
            result <- sqlTestQuery(conn)

          } yield (result)

        for {
          conn      <- ZIO.service[Connection]
          ds        <- ZIO.service[DataSource]
          container <- ZIO.service[PostgreSQLContainer]
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
      }.provideLayerShared(
        ZPostgreSQLContainer.Settings.default >>> ZPostgreSQLContainer.live
      )
    )
}
