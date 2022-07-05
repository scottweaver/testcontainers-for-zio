package io.github.scottweaver.zio.testcontainers.cassandra

import zio._
import com.dimafeng.testcontainers.CassandraContainer
import java.net.InetSocketAddress
import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.internal.core.metadata.DefaultEndPoint
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
