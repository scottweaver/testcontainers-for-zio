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
import org.testcontainers.utility.DockerImageName
import zio._

import java.sql.{Connection, DriverManager}
import javax.sql.DataSource

object ZPostgreSQLContainer {
  final case class Settings(
    imageVersion: String,
    databaseName: String,
    username: String,
    password: String,
    imageName: String = "postgres"
  )

  object Settings {
    val default: ULayer[Has[Settings]] = ZLayer.succeed(
      Settings(
        "latest",
        PostgreSQLContainer.defaultDatabaseName,
        PostgreSQLContainer.defaultUsername,
        PostgreSQLContainer.defaultPassword
      )
    )
  }

  type Provides = Has[JdbcInfo] with Has[Connection] with Has[DataSource] with Has[PostgreSQLContainer]

  val live: ZLayer[Has[Settings], Nothing, Provides] = {

    def makeManagedConnection(container: PostgreSQLContainer) =
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

    def makeManagedContainer(settings: Settings) =
      ZManaged.make(
        ZIO.effect {
          val containerDef = PostgreSQLContainer.Def(
            dockerImageName = DockerImageName
              .parse(s"${settings.imageName}:${settings.imageVersion}")
              .asCompatibleSubstituteFor("postgres"),
            databaseName = settings.databaseName,
            username = settings.username,
            password = settings.password
          )
          containerDef.start()
        }.orDie
      )(container =>
        ZIO
          .effect(container.stop())
          .tapError(err => ZIO.effect(s"Error stopping container: $err"))
          .ignore
      )

    ZLayer.fromManagedMany {
      for {
        settings  <- ZIO.service[Settings].toManaged_
        container <- makeManagedContainer(settings)
        conn      <- makeManagedConnection(container)
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

        Has(jdbcInfo) ++ Has[Connection](conn) ++ Has[DataSource](dataSource) ++ Has(container)
      }
    }
  }
}
