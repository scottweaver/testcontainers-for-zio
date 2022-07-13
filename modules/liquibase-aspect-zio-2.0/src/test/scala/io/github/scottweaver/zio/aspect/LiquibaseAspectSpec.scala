package io.github.scottweaver.zio.aspect

import zio.test._
import zio._
import zio.test.TestAspect._
import io.github.scottweaver.zio.testcontainers.postgres.ZPostgreSQLContainer
import java.sql.Connection

object LiquibaseAspectSpec extends ZIOSpecDefault {

  // For diagnostics if needed.
  val showTables = (ZIO
    .serviceWithZIO[Connection] { conn =>
      ZIO.attemptBlocking {
        val stmt = conn.createStatement()
        val rs   = stmt.executeQuery("SELECT table_name FROM information_schema.tables")

        while (rs.next() == true)
          println(s">>> ${rs.getString("table_name")}")

      }
    })
    .orDie

  val dropTables = TestAspect.after(
    (ZIO
      .serviceWithZIO[Connection] { conn =>
        ZIO.attemptBlocking {
          val stmt = conn.createStatement()
          stmt.executeUpdate("DROP TABLE person, state, databasechangeloglock, databasechangelog")
        }
      })
      .orDie
  )

  val verificationScript = (ZIO
    .serviceWithZIO[Connection] { conn =>
      ZIO.attemptBlocking {
        val stmt  = conn.createStatement()
        stmt.execute("INSERT INTO state (id) VALUES ('TX')")
        stmt.execute(
          "INSERT INTO person (firstname, lastname, state, username) VALUES ('Jane', 'Doe', 'TX', 'jane.doe')"
        )
        val rs    = stmt.executeQuery("SELECT * FROM state")
        rs.next()
        val texas = rs.getString("id")
        val rs1   = stmt.executeQuery("SELECT * FROM person")
        rs1.next()
        val jane  =
          (rs1.getString("firstname"), rs1.getString("lastname"), rs1.getString("state"), rs1.getString("username"))
        (texas, jane)
      }
    })

  val verify =
    verificationScript.map { case ((texas), (firstName, lastName, state, userId)) =>
      assertTrue(
        texas == "TX" &&
          firstName == "Jane" &&
          lastName == "Doe" &&
          state == "TX" &&
          userId == "jane.doe"
      )
    }

  val changeLog = ZLayer.fromZIO(LiquibaseAspect.makeDatabaseChangeLog("classpath://changelog.yaml"))

  def spec =
    suite("LiquibaseAspectSpec")(
      test("Correctly runs the migration using a string as the path to a changelog.")(verify) @@ LiquibaseAspect
        .migrate(
          "classpath://changelog.yaml"
        ) @@ dropTables,
      test("Correctly runs the migration using a DatabaseChangeLog within the environment.")(
        verify
      ) @@ LiquibaseAspect.migrateUsingChangelog @@ dropTables
    )
      .provideShared(
        Scope.default,
        ZPostgreSQLContainer.Settings.default,
        ZPostgreSQLContainer.live,
        LiquibaseAspect.Settings.default,
        changeLog
      ) @@ sequential

}
