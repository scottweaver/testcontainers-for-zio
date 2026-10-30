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

import liquibase.changelog._
import liquibase.database._
import liquibase.database.jvm.JdbcConnection
import liquibase.parser._
import liquibase.resource._
import liquibase.{Contexts, LabelExpression, Liquibase}
import zio._
import zio.test.TestAspect.{before, beforeAll}

import java.sql.Connection
import java.util.logging._

object LiquibaseAspect {

  final case class Settings(
    resourceAccessor: ResourceAccessor,
    contexts: Contexts,
    labelExpression: LabelExpression,
    database: Database,
    logLevel: Level
  )

  /**
   * Correctly creates a liquibase DatabaseChangeLog instance given a valid path
   * to a changelog file.
   *
   * @param pathToChangeLog
   * @param settings
   * @return
   */
  def unsafeMakeDatabaseChangeLog(pathToChangeLog: String, settings: Settings) = {
    // You can't just pass in the path directly to the DatabaseChangeLog constructor.  Instead
    // you have to actually parse the file to generate the DatabaseChangeLog.
    val parser = ChangeLogParserFactory.getInstance().getParser(pathToChangeLog, settings.resourceAccessor)
    val clp    = new ChangeLogParameters(settings.database)
    parser.parse(pathToChangeLog, clp, settings.resourceAccessor)
  }

  def makeDatabaseChangeLog(pathToChangeLog: String) =
    (ZIO
      .serviceWithZIO[Settings] { settings =>
        ZIO.attempt(unsafeMakeDatabaseChangeLog(pathToChangeLog, settings))
      })
      .orDie

  object Settings {
    def makeUnsafeDefault(conn: Connection): Settings = {

      val database = DatabaseFactory
        .getInstance()
        .findCorrectDatabaseImplementation(
          new JdbcConnection(conn)
        )
      Settings(
        resourceAccessor = new ClassLoaderResourceAccessor(),
        contexts = new Contexts(),
        labelExpression = new LabelExpression(),
        database = database,
        logLevel = Level.WARNING
      )
    }

    val default =
      ZLayer.fromZIO(
        ZIO.serviceWithZIO[Connection](conn => ZIO.attempt(makeUnsafeDefault(conn)))
      )

  }

  /**
   * Liquibase using Java logging this scoped ZIO will temporarily set the log
   * level to the value set in `Settings.logLevel`.
   *
   * @return
   */
  private def temporarilyChangeLiquibaseLogLevel =
    ZIO.serviceWithZIO[Settings] { settings =>
      ZIO
        .acquireRelease(
          ZIO.succeed {
            val logger           = Logger.getLogger("liquibase")
            val originalLogLevel = logger.getLevel()
            logger.setLevel(settings.logLevel)
            (logger, originalLogLevel)
          }
        ) { case (logger, level) => ZIO.succeed(logger.setLevel(level)) }
        .unit

    }

  private def doMigrate: URIO[Settings with DatabaseChangeLog with Scope, Unit] = {
    val migration = (for {
      settings <- ZIO.service[Settings]
      cl       <- ZIO.service[DatabaseChangeLog]
      _ <-
        ZIO.attempt {
          val liquibase = new Liquibase(cl, settings.resourceAccessor, settings.database)

          liquibase.update(settings.contexts, settings.labelExpression)
        }
    } yield ()).orDie

    temporarilyChangeLiquibaseLogLevel *> migration
  }

  private def doMigrate(pathToChangelog: String): URIO[Settings with Scope, Unit] =
    for {
      cl <- makeDatabaseChangeLog(pathToChangelog)
      _  <- doMigrate.provideSome[Settings with Scope](ZLayer.succeed(cl))
    } yield ()

  def migrate(pathToChangeLog: String) = before(doMigrate(pathToChangeLog))

  def migrateUsingChangelog = before(doMigrate)

  def migrateOnce(pathToChangelog: String) = beforeAll(doMigrate(pathToChangelog))

  def migrateOnceUsingChangeLog = beforeAll(doMigrate)

}
