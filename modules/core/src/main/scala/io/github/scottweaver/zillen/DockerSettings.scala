package io.github.scottweaver.zillen

import zio._
import zio.prelude._

final case class DockerSettings(
  socketPath: DockerSocketPath,
  inspectContainerPromiseSettings: InspectContainerPromise.Settings,
  readyCheckSettings: ReadyCheck.Settings
) { self =>

  def withInspectContainerPromiseSettings(
    inspectContainerPromiseSettings: InspectContainerPromise.Settings
  ): DockerSettings =
    copy(inspectContainerPromiseSettings = inspectContainerPromiseSettings)

  def withReadyCheckSettings(
    readyCheckSettings: ReadyCheck.Settings
  ): DockerSettings =
    copy(readyCheckSettings = readyCheckSettings)

  def withSocketPath(socketPath: DockerSocketPath): DockerSettings =
    copy(socketPath = socketPath)

}

object DockerSettings {

  object DockerSocketPath extends Subtype[String]

  val socketPath = ZIO.serviceWith[DockerSettings](_.socketPath)

  val defaultPromisedSettings = InspectContainerPromise.Settings(250.millis, 5)
  val readyCheckSettings      = ReadyCheck.Settings(250.millis, 5)

  def default(
    builder: DockerSettings => DockerSettings = identity
  ) =
    ZLayer.fromZIOEnvironment {
      val defaultPath = "/var/run/docker.sock"

      for {
        _ <- validFilePath(defaultPath).mapError(_.asException).orDie
      } yield {
        val settings =
          builder(DockerSettings(DockerSocketPath(defaultPath), defaultPromisedSettings, readyCheckSettings))
        ZEnvironment(
          settings,
          settings.inspectContainerPromiseSettings,
          readyCheckSettings
        )
      }
    }

}
