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

import io.github.scottweaver.models.JdbcInfo
import io.github.scottweaver.zillen.Docker
import io.github.scottweaver.zillen.models._
import io.github.scottweaver.zio.testcontainers.postgresql._
import zio._
import zio.test._

import java.sql.Connection
import javax.sql.DataSource
// import io.github.scottweaver.zillen.ContainerSettings

object PostgresContainerSpec extends ZIOSpecDefault {

  private def imageSpec(imageName: String, imageVersion: String = "latest") =
    test(s"Should start up a $imageName:$imageVersion container that can be queried.") {
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

      val expectedContainerName = ContainerName.unsafeMake(
        "/zio-postgres-test-container"
      ) // Docker adds a preceding slash to the original container name.

      for {
        conn      <- ZIO.service[Connection]
        ds        <- ZIO.service[DataSource]
        container <- ZIO.service[InspectContainerResponse]
        _         <- ZIO.service[JdbcInfo]
        result    <- sqlTestQuery(conn)
        result2   <- sqlTestOnDs(ds)
      } yield assertTrue(
        result == 1,
        result2 == 1,
        container.hostConfig.portBindings.findExternalHostPort(5432, Protocol.TCP).nonEmpty,
        container.name.get == expectedContainerName
        // jdbcInfo.jdbcUrl == container.jdbcUrl,
        // jdbcInfo.username == container.username,
        // jdbcInfo.password == container.password,
        // jdbcInfo.driverClassName == container.driverClassName
      )
    }.provide(
      Scope.default,
      Docker.layer(),
      PostgresContainer.Settings.default(builder = _.copy(imageName = imageName, imageVersion = imageVersion)),
      PostgresContainer.layer
    ) @@ TestAspect.withLiveClock

  def spec =
    suite("PostgresContainerSpec")(
      imageSpec("postgres"),
      imageSpec("timescale/timescaledb", "latest-pg14")
    ) @@ TestAspect.sequential
}
