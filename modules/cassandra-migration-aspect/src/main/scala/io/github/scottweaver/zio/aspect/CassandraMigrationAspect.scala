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

import com.datastax.oss.driver.api.core.CqlSession
import org.cognitor.cassandra.migration.keyspace.Keyspace
import org.cognitor.cassandra.migration.{Database, MigrationConfiguration, MigrationRepository, MigrationTask}
import zio._
import zio.test.TestAspect
import zio.test.TestAspect.{before, beforeAll}

object CassandraMigrationAspect {

  type ConfigurationCallback = (MigrationConfiguration) => MigrationConfiguration

  // MigrationTask is closing the database/session, so it can not be used after
  private final class NoCloseableDatabase(session: CqlSession, configuration: MigrationConfiguration)
      extends Database(session, configuration) {
    override def close(): Unit = ()
  }

  def doMigrate(
    session: CqlSession,
    configuration: MigrationConfiguration,
    repository: MigrationRepository
  ): Task[Unit] =
    ZIO.effect {
      val database = new NoCloseableDatabase(session, configuration)

      val task = new MigrationTask(database, repository)
      task.migrate()
    }

  def migrationRepository(location: String = MigrationRepository.DEFAULT_SCRIPT_PATH): MigrationRepository =
    new MigrationRepository(location)

  def migrationConfiguration(keyspace: String): MigrationConfiguration =
    new MigrationConfiguration().withKeyspace(new Keyspace(keyspace))

  def migrate(
    configuration: MigrationConfiguration,
    repository: MigrationRepository
  ): TestAspect[Nothing, Has[CqlSession], Nothing, Any] =
    before(
      ZIO
        .service[CqlSession]
        .flatMap(session => doMigrate(session, configuration, repository))
        .orDie
    )

  def migrate(
    keyspace: String,
    location: String = MigrationRepository.DEFAULT_SCRIPT_PATH
  ): TestAspect[Nothing, Has[CqlSession], Nothing, Any] =
    migrate(migrationConfiguration(keyspace), migrationRepository(location))

  def migrateOnce(
    configuration: MigrationConfiguration,
    repository: MigrationRepository
  ): TestAspect[Nothing, Has[CqlSession], Nothing, Any] =
    beforeAll(
      ZIO
        .service[CqlSession]
        .flatMap(session => doMigrate(session, configuration, repository))
        .orDie
    )

  def migrateOnce(
    keyspace: String,
    location: String = MigrationRepository.DEFAULT_SCRIPT_PATH
  ): TestAspect[Nothing, Has[CqlSession], Nothing, Any] =
    migrateOnce(migrationConfiguration(keyspace), migrationRepository(location))

}
