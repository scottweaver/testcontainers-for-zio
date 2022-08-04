/*
 * Copyright 2021 io.github.scottweaver
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.scottweaver.zillen

import zio._
import models._
import Command.RemoveContainer._

private[zillen] trait ContainerOps { self: ReadyCheckOps =>

  def createImage(image: Image): DockerIO[Interpreter, Image] =
    Interpreter.run(Command.CreateImage(image))

  def createContainer(create: Command.CreateContainer): DockerIO[Interpreter, CreateContainerResponse] =
    Interpreter.run(create)

  def inspectContainer(containerId: ContainerId): DockerIO[Interpreter, Container] =
    Interpreter.run(Command.InspectContainer(containerId))

  def removeContainer(
    containerId: ContainerId,
    force: Force.Type = Force.yes,
    removeVolumes: Volumes.Type = Volumes.yes
  ): DockerIO[Interpreter, ContainerId] =
    Interpreter.run(Command.RemoveContainer(containerId, force, removeVolumes))

  def startContainer(containerId: ContainerId): DockerIO[Interpreter, ContainerId] =
    Interpreter.run(Command.StartContainer(containerId))

  def stopContainer(containerId: ContainerId): DockerIO[Interpreter, Command.StopContainer.Result] =
    Interpreter.run(Command.StopContainer(containerId))

  def makeScopedContainer[T: Tag](create: Command.CreateContainer) = {
    val acquire = for {
      _              <- createImage(create.image)
      response       <- createContainer(create)
      _              <- startContainer(response.id)
      runningPromise <- readyWhenRunningPromise[T](response.id)
    } yield (response, runningPromise)

    val release: (
      (CreateContainerResponse, Promise[Nothing, Boolean])
    ) => URIO[ContainerSettings[T] with Interpreter with ReadyCheck, Unit] = { case (response, _) =>
      (for {
        _           <- stopContainer(response.id)
        donePromise <- doneWhenDeadOrExitedPromise[T](response.id)
        _           <- removeContainer(response.id)
        _           <- donePromise.await
      } yield ()).mapError(_.asException).orDie
    }

    ZIO.acquireRelease(acquire)(release)

  }
}
