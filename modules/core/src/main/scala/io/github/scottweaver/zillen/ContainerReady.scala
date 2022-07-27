package io.github.scottweaver.zillen

import io.github.scottweaver.zillen.models._
import zio._

trait ReadyCheck {

  def awaitReadyContainer(
    containerId: ContainerId,
    readyCheck: InspectContainerResponse => URIO[Any, Boolean]
  ): DockerIO[Any, Boolean]
}

object ReadyCheck {
  final case class Settings(exponentialBackOffBase: Duration, maxRetries: Int)

  def awaitReadyContainer(
    containerId: ContainerId,
    readyCheck: InspectContainerResponse => URIO[Any, Boolean]
  ) = ZIO.serviceWithZIO[ReadyCheck](_.awaitReadyContainer(containerId, readyCheck))

  val layer = ZLayer.fromZIO(
    for {
      interpreter <- ZIO.service[Interpreter]
      settings    <- ZIO.service[Settings]
    } yield ReadyCheckLive(interpreter, settings.exponentialBackOffBase, settings.maxRetries)
  )

}

final case class ReadyCheckLive(
  interpreter: Interpreter,
  backOffBase: Duration,
  maxRetries: Int
) {

  private val schedule: Schedule[Any, Boolean, Boolean] =
    Schedule.recurUntil[Boolean](identity) <* Schedule.recurs(maxRetries) <* Schedule.exponential(backOffBase)

  def awaitReadyContainer(containerId: ContainerId, readyCheck: InspectContainerResponse => URIO[Any, Boolean]) = {
    val check = Container.inspectContainer(containerId).flatMap(readyCheck).provide(ZLayer.succeed(interpreter))

    check.repeat[Any, Boolean](schedule)
  }

}
