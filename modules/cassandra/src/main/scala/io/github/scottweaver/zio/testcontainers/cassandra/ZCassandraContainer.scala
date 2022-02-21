package io.github.scottweaver.zio.testcontainers.cassandra

import zio._
// import com.datastax.driver.core.Cluster
// import org.testcontainers.containers.{CassandraContainer => JavaCassandraContainer}
// import org.testcontainers.utility.DockerImageName
// import com.testcontainers._
import com.dimafeng.testcontainers.CassandraContainer
import java.net.InetSocketAddress
import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.internal.core.metadata.DefaultEndPoint

object ZCassandraContainer {

  type Provides = Has[CassandraContainer] with Has[CqlSession]
  type Settings = CassandraContainer.Def

  val live: ZLayer[Has[Settings], Nothing, Provides] =
    ZLayer.fromManagedMany {

      def makeContainer(settings: Settings) =
        ZManaged.make(
          ZIO
            .effect(
              settings.createContainer()
            )
            .orDie
        ) { container =>
          ZIO.effect(container.stop()).orDie
        }

      def makeSession(container: CassandraContainer) =
        ZManaged.make(
          ZIO.effect {

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
              .build()
          }.orDie
        )(session => ZIO.effect(session.close()).orDie)

      for {
        settings  <- ZIO.service[Settings].toManaged_
        container <- makeContainer(settings)
        session   <- makeSession(container)
      } yield Has(container) ++ Has(session)
    }

}
