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
