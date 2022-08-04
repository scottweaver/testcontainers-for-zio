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

package io.github.scottweaver.zio.testcontainers.cassandra

import zio._
import zio.test._
import zio.test.Assertion._

object ZCassandraContainerSpec extends DefaultRunnableSpec {

  def spec = suite("ZCassandraContainerSpec")(
    testM("Should start up a Cassandra container, execute against that container and then close it.") {

      val testCase =
        for {
          session <- ZCassandraContainer.session
          rs      <- ZIO.effect(session.execute("select release_version from system.local"))
          row     <- ZIO.effect(rs.one())

        } yield (
          assert(row.getString("release_version"))(equalTo("3.11.2"))
        )

      testCase
    }
  ).provideLayerShared(ZCassandraContainer.Settings.default >>> ZCassandraContainer.live)

}
