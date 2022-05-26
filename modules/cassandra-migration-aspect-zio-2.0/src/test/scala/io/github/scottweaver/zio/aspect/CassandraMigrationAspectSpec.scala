package io.github.scottweaver.zio.aspect

import zio._
import zio.test.Assertion._
import zio.test.TestAspect.sequential
import zio.test._
import com.datastax.oss.driver.api.core.CqlSession
import io.github.scottweaver.zio.testcontainers.cassandra.ZCassandraContainer
import org.cognitor.cassandra.migration.keyspace.Keyspace

import java.util.UUID

object CassandraMigrationAspectSpec extends ZIOSpecDefault {

  def spec = suite("CassandraMigrationAspect")(
    test("Should run Cassandra migrations from the default location e.g. 'classpath:cassandra/migration'.") {

      def testInsert(session: CqlSession) = ZIO.fromCompletionStage {
        session.executeAsync(s"INSERT INTO person (id, name) VALUES (${UUID.randomUUID().toString}, 'Foo')")
      }

      val testCase = for {
        session <- ZIO.service[CqlSession]
        res     <- testInsert(session)
      } yield assert(res.wasApplied())(equalTo(true))

      testCase

    } @@ CassandraMigrationAspect.migrate()(_.withKeyspace(new Keyspace("test"))),
    test("Should run Cassandra migrations from the specified location.") {

      def testInsert(session: CqlSession) = ZIO.fromCompletionStage {
        session.executeAsync(
          s"INSERT INTO custom_person (id, last_name, first_name) VALUES (${UUID.randomUUID().toString}, 'Doe', 'Jane')"
        )
      }

      val testCase = for {
        session <- ZIO.service[CqlSession]
        res     <- testInsert(session)
      } yield assert(res.wasApplied())(equalTo(true))

      testCase

    } @@ CassandraMigrationAspect.migrate("custom")(_.withKeyspace(new Keyspace("test2")))
  )
    .provideShared(
      ZCassandraContainer.Settings.default,
      ZCassandraContainer.live
    ) @@ sequential
}
