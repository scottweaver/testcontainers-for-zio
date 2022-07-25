package io.github.scottweaver

import zio._
import io.github.scottweaver.zillen.models._

package object zillen {
  type DockerIO[R, A] = ZIO[R, DockerContainerFailure, A]

  type DockerSocketPath = DockerSettings.DockerSocketPath.Type

  type InspectContainerPromise[E, A] = Promise[E, (InspectContainerResponse, A)]

}
