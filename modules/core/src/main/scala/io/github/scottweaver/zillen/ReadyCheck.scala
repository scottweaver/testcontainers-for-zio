package io.github.scottweaver.zillen

import io.github.scottweaver.zillen.models._
import zio._

trait ReadyCheck {

  def makePromise[R](
    containerId: ContainerId,
    readyCheck: InspectContainerResponse => ZIO[R, Throwable, Boolean],
    settings: ReadyCheck.Settings
  ): DockerIO[R, Promise[Nothing, Boolean]]
}

object ReadyCheck {

  sealed trait Settings {
    def exponentialBackOffBase: Duration
    def maxRetries: Int
  }

  final case class ContainerReady(exponentialBackOffBase: Duration, maxRetries: Int) extends Settings

  final case class ContainerRunning(exponentialBackOffBase: Duration, maxRetries: Int) extends Settings

  val layer = ZLayer.fromZIO(ZIO.serviceWith[Interpreter](ReadyCheckLive(_)))

}

final case class ReadyCheckLive(
  interpreter: Interpreter
) extends ReadyCheck {

  def makePromise[R](
    containerId: ContainerId,
    readyCheck: InspectContainerResponse => ZIO[R, Throwable, Boolean],
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
      .provide(ZLayer.succeed(interpreter))
      .flatMap(insulatedCheck)

    for {
      checkPromise <- Promise.make[Nothing, Boolean]
      _ <- check
             .repeat[R, Boolean](schedule)
             .flatMap(checkPromise.succeed)
             .fork

    } yield checkPromise
  }

}
