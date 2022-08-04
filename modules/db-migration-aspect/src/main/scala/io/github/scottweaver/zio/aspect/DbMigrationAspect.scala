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

package io.github.scottweaver.zio.aspect

import io.github.scottweaver.models.JdbcInfo
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.configuration.FluentConfiguration
import zio._
import zio.test.TestAspect.{before, beforeAll}

object DbMigrationAspect {

  type ConfigurationCallback = (FluentConfiguration) => FluentConfiguration

  private def doMigrate(jdbcInfo: JdbcInfo, configureCallback: ConfigurationCallback, locations: String*) =
    ZIO.effect {
      val flyway = configureCallback({
        val flyway = Flyway
          .configure()
          .dataSource(jdbcInfo.jdbcUrl, jdbcInfo.username, jdbcInfo.password)

        if (locations.nonEmpty)
          flyway.locations(locations: _*)
        else
          flyway
      })
        .load()
      flyway.migrate
    }

  def migrate(migrationLocations: String*)(configureCallback: ConfigurationCallback = identity) = before(
    ZIO
      .service[JdbcInfo]
      .flatMap(jdbcInfo => doMigrate(jdbcInfo, configureCallback, migrationLocations: _*))
      .orDie
  )

  def migrateOnce(migrationLocations: String*)(configureCallback: ConfigurationCallback = identity) =
    beforeAll(
      ZIO
        .service[JdbcInfo]
        .flatMap(jdbcInfo => doMigrate(jdbcInfo, configureCallback, migrationLocations: _*))
        .orDie
    )

}
