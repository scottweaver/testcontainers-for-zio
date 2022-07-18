package io.github.scottweaver.zio.testcontainers.k3s

import org.testcontainers.k3s.K3sContainer
import org.testcontainers.utility.DockerImageName
import zio.{ ULayer, ZIO, ZLayer }

object ZK3sContainer {
  val defaultImage = "rancher/k3s"
  val defaultTag   = "v1.21.3-k3s1"

  val defaultDockerImageName = s"$defaultImage:$defaultTag"

  case class Settings(dockerImageName: DockerImageName = DockerImageName.parse(defaultDockerImageName)) {
    def createContainer(): K3sContainer = new K3sContainer(dockerImageName)
  }

  object Settings {
    val default: ULayer[Settings] = ZLayer.succeed(Settings())
  }

  val live: ZLayer[Settings, Nothing, K3sContainer] = {
    def makeContainer(settings: Settings) =
      ZIO.acquireRelease(ZIO.attempt {
        val container = settings.createContainer()
        container.start()
        container
      }.orDie)(container =>
        ZIO
          .attempt(container.stop())
          .ignoreLogged
      )

    ZLayer.scoped {
      for {
        settings  <- ZIO.service[Settings]
        container <- makeContainer(settings)
      } yield container
    }
  }

  val default: ZLayer[Any, Nothing, K3sContainer] = Settings.default >>> live
}
