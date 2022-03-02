package io.github.scottweaver.zio.testcontainers.cassandra

import zio._
import zio.test._
import zio.test.Assertion._

object ZCassandraContainerSpec extends DefaultRunnableSpec {

  def spec = suite("ZCassandraContainerSpec")(
    test("Should start up a Cassandra container, execute against that container and then close it.") {

      val testCase =
        for {
          session <- ZCassandraContainer.session
          rs      <- ZIO.attempt(session.execute("select release_version from system.local"))
          row     <- ZIO.attempt(rs.one())

        } yield (
          assert(row.getString("release_version"))(equalTo("3.11.2"))
        )

      testCase
    }
  ).provideShared(ZCassandraContainer.Settings.default, ZCassandraContainer.live)

}
