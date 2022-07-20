package io.github.scottweaver.zillen

import zio._
import zio.prelude._
import java.nio.file._

final case class DockerSettings(
  socketPath: DockerSocketPath
)

object DockerSettings {

  object DockerSocketPath extends Subtype[String]

  val socketPath = ZIO.serviceWith[DockerSettings](_.socketPath)

  val default: ZLayer[Any, DockerContainerFailure, DockerSettings] =
    ZLayer.fromZIO {
      val defaultPath = "/var/run/docker.sock"

      val validPath: DockerIO[Any, Path] = ZIO.attempt(Paths.get(defaultPath)).catchAll {
        case ipe: InvalidPathException =>
          ZIO.fail(
            DockerContainerFailure.InvalidDockerConfiguration(
              s"'${defaultPath}' does not appear to be a valid file system path.",
              Some(ipe)
            )
          )
        case t: Throwable => ZIO.die(t)
      }

      def pathExists(path: Path) = ZIO.attempt(Files.exists(path)).orDie.flatMap { exists =>
        if (!exists)
          ZIO.fail(
            DockerContainerFailure.InvalidDockerRuntimeState(
              s"The docker socket file, `${defaultPath}`, does not exist. Please verify that Docker is installed or you have started the Docker Desktop application.",
              None
            )
          )
        else ZIO.unit
      }

      (for {
        path <- validPath
        _    <- pathExists(path)
      } yield DockerSettings(DockerSocketPath("/var/run/docker.sock")))
    }

}
