package io.github.scottweaver.zillen

import io.github.scottweaver.zillen.models._
import zio._
import scala.annotation.nowarn

trait ReadyCheck {

  def makePromise(
    containerId: ContainerId,
    readyCheck: InspectContainerResponse => ZIO[Any, Throwable, Boolean],
    settings: ReadyCheck.Settings
  ): DockerIO[Any, Promise[Nothing, Boolean]]
}

object ReadyCheck {

  sealed trait Settings {
    def exponentialBackOffBase: Duration
    def maxRetries: Int
  }

  final case class ContainerReady(exponentialBackOffBase: Duration, maxRetries: Int) extends Settings

  final case class ContainerRunning(exponentialBackOffBase: Duration, maxRetries: Int) extends Settings

  @nowarn
  def makePromise[T: Tag](
    containerId: ContainerId,
    check: InspectContainerResponse => ZIO[Any, Throwable, Boolean]
  ): DockerIO[ContainerSettings[T] with ReadyCheck, Promise[Nothing, Boolean]] = for {
    settings   <- ZIO.serviceWith[ContainerSettings[T]](_.readyCheckSettings)
    readyCheck <- ZIO.service[ReadyCheck]
    ready      <- readyCheck.makePromise(containerId, check, settings)
  } yield ready

  val layer = ZLayer.fromZIO(ZIO.serviceWith[Interpreter](ReadyCheckLive(_)))

}

final case class ReadyCheckLive(
  interpreter: Interpreter
) extends ReadyCheck {

  def makePromise(
    containerId: ContainerId,
    readyCheck: InspectContainerResponse => ZIO[Any, Throwable, Boolean],
    settings: ReadyCheck.Settings
  ) = {
    import settings._
    val schedule: Schedule[Any, Boolean, Boolean] =
      Schedule.recurUntil[Boolean](identity) <* Schedule.recurs(maxRetries) <* Schedule.exponential(base =
        exponentialBackOffBase
      )

    def insulatedCheck(id: InspectContainerResponse) = readyCheck(id).catchAll { e =>
      ZIO.logWarning(s"Retrying failed ready check. Cause: ${e.getMessage}.") *> ZIO.succeed(false)
    }

    val check = Docker
      .inspectContainer(containerId)
      .flatMap(insulatedCheck)
      .provide(ZLayer.succeed(interpreter))

    for {
      checkPromise <- Promise.make[Nothing, Boolean]
      _ <- check
             .repeat[Any, Boolean](schedule)
             .flatMap(checkPromise.succeed)
             .fork

    } yield checkPromise
  }

}
