package io.github.scottweaver.zio.testcontainers.cassandra

import zio._
import com.dimafeng.testcontainers.CassandraContainer
import java.net.InetSocketAddress
import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.internal.core.metadata.DefaultEndPoint

object ZCassandraContainer {

  // type Provides = Has[CassandraContainer] with Has[CqlSession]
  type Settings = CassandraContainer.Def

  object Settings {
    val default = ZLayer.succeed(CassandraContainer.Def())
  }

  val session = ZIO.service[CqlSession]

  val live = {
      def makeContainer(settings: Settings) =
        ZIO.acquireRelease (
          ZIO.attempt {
            val container = settings.createContainer()
            container.start()
            container
          }.orDie
        ) { container =>
          ZIO.attempt(container.stop()).orDie
        }

      def makeSession(container: CassandraContainer) =
        ZIO.acquireRelease (
          ZIO.attempt {
            CqlSession.builder
              .addContactEndPoint(
                new DefaultEndPoint(
                  InetSocketAddress
                    .createUnresolved(
                      container.cassandraContainer.getContainerIpAddress,
                      container.cassandraContainer.getFirstMappedPort.intValue()
                    )
                )
              )
              .withLocalDatacenter("datacenter1")
              .build()
          }.orDie
        )(session => ZIO.attempt(session.close()).orDie)

    ZLayer.scopedEnvironment {
      for {
        settings  <- ZIO.service[Settings]
        container <- makeContainer(settings)
        session   <- makeSession(container)
      } yield ZEnvironment(container, session)
    }
  }

}
