package io.github.scottweaver.zio.testcontainers.k3s

import org.testcontainers.k3s.K3sContainer
import org.testcontainers.utility.DockerImageName
import zio.{ Has, ULayer, ZIO, ZLayer, ZManaged }

object ZK3sContainer {
  val defaultImage = "rancher/k3s"
  val defaultTag   = "v1.21.3-k3s1"

  val defaultDockerImageName = s"$defaultImage:$defaultTag"

  case class Settings(dockerImageName: DockerImageName = DockerImageName.parse(defaultDockerImageName)) {
    def createContainer(): K3sContainer = new K3sContainer(dockerImageName)
  }

  object Settings {
    val default: ULayer[Has[Settings]] = ZLayer.succeed(Settings())
  }

  val live: ZLayer[Has[Settings], Nothing, Has[K3sContainer]] = {
    def makeContainer(settings: Settings) =
      ZManaged.make(
        ZIO.effect {
          val container = settings.createContainer()
          container.start()
          container
        }.orDie
      )(container =>
        ZIO
          .effect(container.stop())
          .tapError(e => ZIO.effect(println(s"Error stopping container: $e")))
          .ignore
      )

    ZLayer.fromManaged {
      for {
        settings  <- ZIO.service[Settings].toManaged_
        container <- makeContainer(settings)
      } yield container
    }
  }

  val default: ZLayer[Any, Nothing, Has[K3sContainer]] = Settings.default >>> live
}
