package io.github.scottweaver.zio.aspect

import com.datastax.oss.driver.api.core.CqlSession
import org.cognitor.cassandra.migration.{ Database, MigrationConfiguration, MigrationRepository, MigrationTask }
import zio._
import zio.test.TestAspect.{ before, beforeAll }

object CassandraMigrationAspect {

  type ConfigurationCallback = (MigrationConfiguration) => MigrationConfiguration

  // MigrationTask is closing the database/session, so it can not be used after
  private final class NoCloseableDatabase(session: CqlSession, configuration: MigrationConfiguration)
      extends Database(session, configuration) {
    override def close(): Unit = ()
  }

  private def doMigrate(cqlSession: CqlSession, configureCallback: ConfigurationCallback, location: String) =
    ZIO.effect {
      val configuration = configureCallback(new MigrationConfiguration)

      val database   = new NoCloseableDatabase(cqlSession, configuration)
      val repository = new MigrationRepository(location)

      val task = new MigrationTask(database, repository)

      task.migrate()
    }

  def migrate(
    mirgationLocation: String = MigrationRepository.DEFAULT_SCRIPT_PATH
  )(configureCallback: ConfigurationCallback = identity) =
    before(
      ZIO
        .service[CqlSession]
        .flatMap(cqlSession => doMigrate(cqlSession, configureCallback, mirgationLocation))
        .orDie
    )

  def migrateOnce(
    migrationLocation: String = MigrationRepository.DEFAULT_SCRIPT_PATH
  )(configureCallback: ConfigurationCallback = identity) =
    beforeAll(
      ZIO
        .service[CqlSession]
        .flatMap(cqlSession => doMigrate(cqlSession, configureCallback, migrationLocation))
        .orDie
    )

}
