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
import models.State

trait ReadyCheckOps {

  def makeReadyCheckPromise[R, T: Tag](
    containerId: ContainerId,
    check: Container => ZIO[R, Throwable, Boolean]
  ): DockerIO[ContainerSettings[T] with ReadyCheck with R, Promise[Nothing, Boolean]] = for {
    settings   <- ZIO.serviceWith[ContainerSettings[T]](_.readyCheckSettings)
    readyCheck <- ZIO.service[ReadyCheck]
    ready      <- readyCheck.makePromise[R](containerId, check, settings)
  } yield ready

  def makeRunningCheckPromise[T: Tag](
    containerId: ContainerId,
    check: Container => ZIO[Any, Throwable, Boolean]
  ): DockerIO[ContainerSettings[T] with ReadyCheck, Promise[Nothing, Boolean]] = for {
    settings   <- ZIO.serviceWith[ContainerSettings[T]](_.readyCheckSettings)
    readyCheck <- ZIO.service[ReadyCheck]
    ready      <- readyCheck.makePromise(containerId, check, settings)
  } yield ready

  def readyWhenStatusPromise[T: Tag](
    containerId: ContainerId,
    statuses: State.Status*
  ): DockerIO[ContainerSettings[T] with ReadyCheck, Promise[Nothing, Boolean]] =
    makeReadyCheckPromise(containerId, c => ZIO.succeed(statuses.contains(c.state.status)))

  def readyWhenRunningPromise[T: Tag](
    containerId: ContainerId
  ): DockerIO[ContainerSettings[T] with ReadyCheck, Promise[Nothing, Boolean]] =
    readyWhenStatusPromise(containerId, Docker.status.Running)

  def doneWhenDeadOrExitedPromise[T: Tag](
    containerId: ContainerId
  ): DockerIO[ContainerSettings[T] with ReadyCheck, Promise[Nothing, Boolean]] =
    readyWhenStatusPromise(containerId, Docker.status.Exited, Docker.status.Dead)

}
