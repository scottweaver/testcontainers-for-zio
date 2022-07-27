package io.github.scottweaver.zillen

import io.github.scottweaver.zillen.models._
import zio._
import State.Status
import Status._

object InspectContainerPromise {

  final case class Settings(exponentialBackOffBase: Duration, maxRetries: Int)

  type ScheduleContext[A] = (InspectContainerResponse, A)

  private def makeSchedule[A](base: Duration, maxRetries: Int)(
    readyIf:  A => Boolean
  ): Schedule[Any, ScheduleContext[A], ScheduleContext[A]] =
    Schedule
      .recurUntil[ScheduleContext[A]] { case (_, a) => readyIf(a) } <* Schedule.exponential(base) <* Schedule.recurs(
      maxRetries
    )

  def readyWhenA[A](containerId: ContainerId, toA: InspectContainerResponse => A)(readyIf: A => Boolean) = {
    val toAPLus = (resp: InspectContainerResponse) => resp -> toA(resp)

    val status = ZIO.debug(s">>> Calling InspectContainer") *> Container
      .inspectContainer(containerId)
      .map(toAPLus) <* ZIO.debug(s">>> InspectContainer returned")

    for {
      settings <- ZIO.service[Settings]
      ready    <- Promise.make[DockerContainerFailure, ScheduleContext[A]]
      _ <-
        status
          .repeat(makeSchedule[A](settings.exponentialBackOffBase, settings.maxRetries)(readyIf))
          .flatMap(ready.succeed)
          // .flatMap { case r @ (_, b) => 
          //   readyIf(b) match {
          //     case true => ready.succeed(r)
          //     case false => ready.succeed(r)
          //   }

          // }
          .tapError(cf => ready.fail(cf))
          .fork
    } yield ready

  }

  def whenReady(containerId: ContainerId, readyIf: InspectContainerResponse => Boolean) =
    readyWhenA[InspectContainerResponse](containerId, identity)(readyIf)

  def whenStatusIs(containerId: ContainerId, readyIf: Status*) =
    readyWhenA[Status](containerId, _.state.status)(readyIf.contains)

  def whenRunning(containerId: ContainerId) =
    whenStatusIs(containerId, Running)

  def whenDeadOrExited(containerId: ContainerId) =
    whenStatusIs(containerId, Dead, Exited)

}
