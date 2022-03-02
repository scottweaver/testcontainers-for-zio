package io.github.scottweaver.zio.aspect

import zio._
import zio.test._
import zio.test.Assertion._
import TestAspect.sequential
import io.github.scottweaver.zio.testcontainers.mysql.ZMySQLContainer
import java.sql.Connection

object DbMigrationAspectSpec extends DefaultRunnableSpec {
  def spec = suite("DatabaseMigrationAspect")(
    test("Should run Flyway migrations from the default location e.g. 'classpath:db/migration'.") {

      def testInsert(conn: Connection) = ZIO.attempt {
        val stmt  = conn.createStatement()
        val count = stmt.executeUpdate("INSERT INTO person (name) VALUES ('Foo')")
        stmt.close()
        count
      }

      val testCase = for {
        conn  <- ZIO.service[Connection]
        count <- testInsert(conn)
      } yield assert(count)(equalTo(1))

      testCase

    } @@ DbMigrationAspect.migrate()(),
    test("Should run Flyway migrations from the specified location.") {

      def testInsert(conn: Connection) = ZIO.attempt {
        val stmt  = conn.createStatement()
        val count = stmt.executeUpdate("INSERT INTO custom_person (last_name, first_name) VALUES ('Doe', 'Jane')")
        stmt.close()
        count
      }

      val testCase = for {
        conn  <- ZIO.service[Connection]
        count <- testInsert(conn)
      } yield assert(count)(equalTo(1))

      testCase

    } @@ DbMigrationAspect.migrate("custom")(_.ignoreMigrationPatterns("*:missing")),
    test("Should run Flyway migrations from the specified location that is set using a callabck.") {

      def testInsert(conn: Connection) = ZIO.attempt {
        val stmt  = conn.createStatement()
        val count = stmt.executeUpdate("INSERT INTO pet (name, species) VALUES ('Goose', 'Dog')")
        stmt.close()
        count
      }

      val testCase = for {
        conn  <- ZIO.service[Connection]
        count <- testInsert(conn)
      } yield assert(count)(equalTo(1))

      testCase

    } @@ DbMigrationAspect.migrate()(_.locations("custom_callback").ignoreMigrationPatterns("*:missing"))
  )
    .provideShared(
      ZMySQLContainer.Settings.default,
      ZMySQLContainer.live
    ) @@ sequential
}
