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

  val live =
    ZLayer.fromManagedEnvironment {

      def makeContainer(settings: Settings) =
        ZManaged.acquireReleaseWith(
          ZIO
            .attempt {
             val container = settings.createContainer()
             container.start()
             container
            }
            .orDie
        ) { container =>
          ZIO.attempt(container.stop()).orDie
        }

      def makeSession(container: CassandraContainer) =
        ZManaged.acquireReleaseWith(
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

      for {
        settings  <- ZIO.service[Settings].toManaged
        container <- makeContainer(settings)
        session   <- makeSession(container)
      } yield  ZEnvironment(container, session)
    }

}
