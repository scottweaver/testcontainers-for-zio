package io.github.scottweaver.zio.aspect

import liquibase.database._
import zio._
import zio.test.TestAspect.{ before, beforeAll }
import java.sql.Connection
import liquibase.database.jvm.JdbcConnection
import liquibase._
import liquibase.resource.ClassLoaderResourceAccessor

object LiquibaseAspect {

  final case class Settings(
    classLoaderResourceAccessor: ClassLoaderResourceAccessor,
    contexts: Contexts,
    labelExpression: LabelExpression
  )

  object Settings {
    val default = ZLayer.succeed(
      Settings(
        classLoaderResourceAccessor = new ClassLoaderResourceAccessor(),
        contexts = new Contexts(),
        labelExpression = new LabelExpression()
      )
    )

  }

  private def doMigrate(pathToChangeLog: String) =
    for {
      conn     <- ZIO.service[Connection]
      settings <- ZIO.service[Settings]
      _        <-
        ZIO.attempt {
          val database  = DatabaseFactory
            .getInstance()
            .findCorrectDatabaseImplementation(
              new JdbcConnection(conn)
            )
          val liquibase = new Liquibase(pathToChangeLog, settings.classLoaderResourceAccessor, database)
          liquibase.update(settings.contexts, settings.labelExpression)
        }
    } yield ()

  def migrate(pathToChangelog: String) = before(doMigrate(pathToChangelog))

  def migrateOnce(pathToChangelog: String) = beforeAll(doMigrate(pathToChangelog))

}
