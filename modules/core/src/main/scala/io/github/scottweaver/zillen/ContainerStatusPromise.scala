package io.github.scottweaver.zillen

import io.github.scottweaver.zillen.models._
import zio._
import State.Status
import Status._

object ContainerStatusPromise {

  final case class Settings(exponentialBackoffBase: Duration, maxRetries: Int)

  object Settings {
    val default = ZLayer.succeed(Settings(250.millis, 10))
  }

  private def makeSchedule[A](base: Duration, maxRetries: Int)(
    readyIf: A => Boolean
  ): Schedule[Any, A, A] =
    (Schedule
      .recurUntil[A](readyIf(_)) && Schedule.exponential(base) && Schedule.recurs(maxRetries)) *> Schedule.identity

  def readyWhenA[A](containerId: ContainerId, toA: InspectContainerResponse => A)(readyIf: A => Boolean) = {
    val status = ZIO.debug(s">>> Calling InspectContainer") *> Container
      .inspectContainer(containerId)
      .tap(icr => ZIO.debug(s">>> InspectContainerResponse('${containerId}'): $icr"))
      .map(toA) <* ZIO.debug(s">>> InspectContainer returned")

    for {
      settings <- ZIO.service[Settings]
      ready    <- Promise.make[DockerContainerFailure, A]
      _ <-
        status
          .repeat(makeSchedule[A](settings.exponentialBackoffBase, settings.maxRetries)(readyIf))
          .flatMap(ready.succeed)
          .tapError(cf => ready.fail(cf))
          .fork
    } yield ready

  }

  def readyWhen(containerId: ContainerId, readyIf: InspectContainerResponse => Boolean) =
    readyWhenA[InspectContainerResponse](containerId, identity)(readyIf)

  def readyWhenStatus(containerId: ContainerId, readyIf: Status*) =
    readyWhenA[Status](containerId, _.state.status)(readyIf.contains)

  def readyRunningDeadOrExited(containerId: ContainerId) =
    readyWhenStatus(containerId, Running, Dead, Exited)

  def deadOrExited(containerId: ContainerId) =
    readyWhenStatus(containerId, Dead, Exited)

}
