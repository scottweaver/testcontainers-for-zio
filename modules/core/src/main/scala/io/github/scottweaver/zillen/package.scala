package io.github.scottweaver

import zio._
import io.github.scottweaver.zillen.models._
import java.nio.file._

package object zillen {


  type DockerIO[R, A] = ZIO[R, DockerContainerFailure, A]

  type DockerSocketPath = DockerSettings.DockerSocketPath.Type

  type InspectContainerPromise[E, A] = Promise[E, (InspectContainerResponse, A)]

  private[zillen] def validFilePath(path: String): DockerIO[Any, Path] = {
    val validPath: DockerIO[Any, Path] = ZIO.attempt(Paths.get(path)).catchAll {
      case ipe: InvalidPathException =>
        ZIO.fail(
          DockerContainerFailure
            .fromConfigurationException(s"'${path}' does not appear to be a valid file system path.", ipe)
        )
      case t: Throwable => ZIO.die(t)
    }

    def pathExists(path: Path) = ZIO.attempt(Files.exists(path)).orDie.flatMap { exists =>
      if (!exists)
        ZIO.fail(
          DockerContainerFailure.InvalidDockerRuntimeState(
            s"The docker socket file, `${path}`, does not exist. Please verify that Docker is installed or you have started the Docker Desktop application.",
            None
          )
        )
      else ZIO.unit
    }

    for {
      path <- validPath
      _    <- pathExists(path)
    } yield path
  }

}
