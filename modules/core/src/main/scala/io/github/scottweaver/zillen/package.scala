package io.github.scottweaver

import zio._

package object zillen {
  type DockerIO[R, A] = ZIO[R, DockerContainerFailure, A]

  type DockerSocketPath = DockerSettings.DockerSocketPath.Type

}
