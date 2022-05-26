package io.github.scottweaver.zio.aspect

import com.datastax.oss.driver.api.core.CqlSession
import org.cognitor.cassandra.migration.keyspace.Keyspace
import org.cognitor.cassandra.migration.{ Database, MigrationConfiguration, MigrationRepository, MigrationTask }
import zio._
import zio.test.TestAspect
import zio.test.TestAspect.{ before, beforeAll }

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
