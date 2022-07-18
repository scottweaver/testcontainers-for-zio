package io.github.scottweaver.zillen

import io.github.scottweaver.zillen.models._
import zio.json._
import zio._
import zio.prelude.Subtype
sealed trait Command {
  type Response

  def makeResponse(statusCode: Int, body: String): ZIO[Any, Throwable, Response]
}

object Command {

  final case class CreateImage(image: Image) extends Command {

    type Response = Image

    override def makeResponse(statusCode: Int, body: String) =
      if (statusCode == 201 || statusCode == 200)
        ZIO.succeed(image)
      else
        ZIO.fail(
          new Exception(
            s"Attempt to create image failed with a status code of ${statusCode}.  See logs for additional details."
          )
        )
  }

  final case class CreateContainer(env: Env, exposedPorts: Port.Exposed, hostConfig: HostConfig, image: Image)
      extends Command {

    type Response = CreateContainerResponse

    override def makeResponse(statusCode: Int, body: String) =
      statusCode match {
        case 201   => ZIO.fromEither(body.fromJson[CreateContainerResponse].left.map(new IllegalArgumentException(_)))
        case 404   =>
          ZIO.fail(
            new IllegalArgumentException(s"Failed to create container as the image, '${image}', does not exist.")
          )
        case other =>
          ZIO.fail(
            new IllegalStateException(
              s"Attempt to create the container failed with a status code ${other}.  Response body: $body"
            )
          )
      }

  }

  final case class RemoveContainer(
    containerId: ContainerId,
    force: RemoveContainer.Force.Type,
    volumes: RemoveContainer.Volumes.Type
  ) extends Command {

    type Response = ContainerId

    override def makeResponse(statusCode: Int, body: String) =
      statusCode match {
        case 204   => ZIO.succeed(containerId)
        case 404   =>
          ZIO.fail(
            new IllegalArgumentException(s"Attempt to remove container with id '${containerId}' failed does not exist.")
          )
        case other =>
          ZIO.fail(
            new IllegalStateException(
              s"Attempt to remove the container failed with a status code ${other}.  Response body: $body"
            )
          )
      }
  }

  object RemoveContainer {
    object Force extends Subtype[Boolean] {
      val yes = wrap(true)
      val no  = wrap(false)
    }
    type Force = Force.type

    object Volumes extends Subtype[Boolean] {
      val yes = wrap(true)
      val no  = wrap(false)
    }
    type Volumes = Volumes.type

  }

  final case class StartContainer(containerId: ContainerId) extends Command {
    type Response = ContainerId

    override def makeResponse(statusCode: Int, body: String) =
      if (statusCode == 204)
        ZIO.succeed(containerId)
      else
        ZIO.fail(
          new Exception(
            s"Attempt to start container, '${containerId}', failed with a status code of ${statusCode}. Response body: $body"
          )
        )
  }

  final case class StopContainer(containerId: ContainerId)  extends Command {
    type Response = StopContainer.Result

    override def makeResponse(statusCode: Int, body: String) =
      statusCode match {
        case 204   => ZIO.succeed(StopContainer.Stopped(containerId))
        case 304   => ZIO.succeed(StopContainer.NotRunning(containerId))
        case 404   =>
          ZIO.fail(
            new IllegalArgumentException(
              s"Attempt to stop container, '${containerId}', failed as it does not exist."
            )
          )
        case other =>
          ZIO.fail(
            new IllegalStateException(
              s"Attempt to stop container , '${containerId}', failed with a status code of ${other}. Response body: $body"
            )
          )
      }
  }

  object StopContainer {
    sealed trait Result extends Any

    final case class Stopped(containerId: ContainerId)    extends AnyVal with Result
    final case class NotRunning(containerId: ContainerId) extends AnyVal with Result
  }

}
