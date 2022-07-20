package io.github.scottweaver.zillen

import zio._
import io.github.scottweaver.zillen.models.ContainerId
import io.github.scottweaver.zillen.models._

/**
 *   1. Create the container 2. Return universal information about the container
 *      e.g exposed ports, exposed environment variables, etc.
 */
object Container {

  def createContainer(create: Command.CreateContainer) =
    ZIO.serviceWithZIO[Interpreter](_.run(create))

  def inspectContainer(containerId: ContainerId): DockerIO[Interpreter, InspectContainerResponse] =
    ZIO
      .serviceWithZIO[Interpreter](_.run(Command.InspectContainer(containerId)))

}
