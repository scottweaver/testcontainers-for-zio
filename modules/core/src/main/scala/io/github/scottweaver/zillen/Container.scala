package io.github.scottweaver.zillen

import zio._
import io.github.scottweaver.zillen.models._

/**
 *   1. Create the container 2. Return universal information about the container
 *      e.g exposed ports, exposed environment variables, etc.
 */
object Container {

  def createImage(image: Image): DockerIO[Interpreter, Image] =
    Interpreter.run(Command.CreateImage(image))

  def createContainer(create: Command.CreateContainer): DockerIO[Interpreter, CreateContainerResponse] =
    Interpreter.run(create)

  def inspectContainer(containerId: ContainerId): DockerIO[Interpreter, InspectContainerResponse] =
    Interpreter.run(Command.InspectContainer(containerId))

  def stopContainer(containerId: ContainerId): DockerIO[Interpreter, Command.StopContainer.Result] =
    Interpreter.run(Command.StopContainer(containerId))

  def makeScopedContainer(create: Command.CreateContainer) = {
    val acquire = for {
      _        <- createImage(create.image)
      response <- createContainer(create)
      _        <- ContainerPromise.whenRunning(response.id)
    } yield response

    val release = (response: CreateContainerResponse) =>
      (for {
        _ <- stopContainer(response.id)
        _ <- ContainerPromise.whenDeadOrExited(response.id)
      } yield ()).mapError(_.asException).orDie

    ZIO.acquireRelease(acquire)(release)

  }
}
