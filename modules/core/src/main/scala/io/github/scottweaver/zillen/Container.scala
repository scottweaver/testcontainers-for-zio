package io.github.scottweaver.zillen

import zio._
import io.github.scottweaver.zillen.models._
import Command.RemoveContainer._

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

  def removeContainer(containerId: ContainerId, force: Force.Type = Force.yes, removeVolumes: Volumes.Type = Volumes.yes): DockerIO[Interpreter, ContainerId] =
    Interpreter.run(Command.RemoveContainer(containerId, force, removeVolumes))

  def startContainer(containerId: ContainerId): DockerIO[Interpreter, ContainerId] =
    Interpreter.run(Command.StartContainer(containerId))

  def stopContainer(containerId: ContainerId): DockerIO[Interpreter, Command.StopContainer.Result] =
    Interpreter.run(Command.StopContainer(containerId))

  def makeScopedContainer(create: Command.CreateContainer) = {
    val acquire = for {
      _              <- createImage(create.image)
      response       <- createContainer(create)
      _              <- startContainer(response.id)
      runningPromise <- InspectContainerPromise.whenRunning(response.id)
    } yield (response, runningPromise)

    val release: (
      (CreateContainerResponse, InspectContainerPromise[DockerContainerFailure, State.Status])
    ) => URIO[InspectContainerPromise.Settings with Interpreter, Unit] = { case (response, _) =>
      (for {
        _             <- stopContainer(response.id)
        exitedPromise <- InspectContainerPromise.whenDeadOrExited(response.id)
        _             <- removeContainer(response.id)
        _             <- exitedPromise.await
      } yield ()).mapError(_.asException).orDie
    }

    ZIO.acquireRelease(acquire)(release)

  }
}
