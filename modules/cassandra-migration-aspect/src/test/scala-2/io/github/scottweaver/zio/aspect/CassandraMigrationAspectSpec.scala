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
import io.github.scottweaver.zio.testcontainers.cassandra.ZCassandraContainer
import zio._
import zio.magic._
import zio.test.Assertion._
import zio.test.TestAspect.sequential
import zio.test._

import java.util.UUID

object CassandraMigrationAspectSpec extends DefaultRunnableSpec {
  def spec = suite("CassandraMigrationAspect")(
    testM("Should run Cassandra migrations from the default location e.g. 'classpath:cassandra/migration'.") {

      def testInsert(session: CqlSession) = ZIO.fromCompletionStage {
        session.executeAsync(s"INSERT INTO person (id, name) VALUES (${UUID.randomUUID().toString}, 'Foo')")
      }

      val testCase = for {
        session <- ZIO.service[CqlSession]
        res     <- testInsert(session)
      } yield assert(res.wasApplied())(equalTo(true))

      testCase

    } @@ CassandraMigrationAspect.migrate("test"),
    testM("Should run Cassandra migrations from the specified location.") {

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

    } @@ CassandraMigrationAspect.migrate("test2", "custom")
  )
    .injectShared(
      ZCassandraContainer.Settings.default,
      ZCassandraContainer.live
    ) @@ sequential
}
