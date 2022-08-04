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

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.internal.core.metadata.DefaultEndPoint
import com.dimafeng.testcontainers.CassandraContainer
import zio._

import java.net.InetSocketAddress
// import org.testcontainers.utility.DockerImageName

object ZCassandraContainer {

  type Settings = CassandraContainer.Def

  object Settings {
    // val default = ZLayer.succeed(CassandraContainer.Def(dockerImageName = DockerImageName.parse("cassandra:4.0.4")))
    val default = ZLayer.succeed(CassandraContainer.Def())
  }

  val session = ZIO.service[CqlSession]

  val live = {
    def makeContainer(settings: Settings) =
      ZIO.acquireRelease(
        ZIO.attemptBlocking {
          val container = settings.createContainer()
          container.start()
          container
        }.orDie
      ) { container =>
        ZIO.attemptBlocking(container.stop()).orDie
      }

    def makeSession(container: CassandraContainer) =
      ZIO.acquireRelease(
        ZIO.attemptBlocking {
          CqlSession.builder
            .addContactEndPoint(
              new DefaultEndPoint(
                InetSocketAddress
                  .createUnresolved(
                    container.cassandraContainer.getHost(),
                    container.cassandraContainer.getFirstMappedPort.intValue()
                  )
              )
            )
            .withLocalDatacenter("datacenter1")
            .build()
        }.orDie
      )(session => ZIO.attemptBlocking(session.close()).orDie)

    ZLayer.scopedEnvironment {
      for {
        settings  <- ZIO.service[Settings]
        container <- makeContainer(settings)
        session   <- makeSession(container)
      } yield ZEnvironment(container, session)
    }
  }

}
