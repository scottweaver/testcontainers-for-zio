package io.github.scottweaver.zillen

import io.github.scottweaver.zillen.models._
import zio._
import zio.prelude.Subtype

sealed trait Command {
  type Response

  def makeResponse(statusCode: Int, body: String): DockerIO[Any, Response]

}

object Command {

  final case class CreateImage(image: Image) extends Command { self =>

    type Response = Image

    override def makeResponse(statusCode: Int, body: String) =
      if (statusCode == 201 || statusCode == 200)
        ZIO.succeed(image)
      else
        CommandFailure.unexpectedDockerApiError(body, self, statusCode)
  }

  final case class CreateContainer(
    env: Env,
    exposedPorts: Port.Exposed,
    hostConfig: HostConfig,
    image: Image,
    containerName: Option[ContainerName]
  ) extends Command { self =>

    type Response = CreateContainerResponse

    override def makeResponse(statusCode: Int, body: String) =
      statusCode match {
        case 201 =>
          CommandFailure.decodeResponse[CreateContainerResponse](
            body,
            self
          )
        case 404 => CommandFailure.imageNotFound(self, image)
        case 409 =>
          CommandFailure
            .decodeResponse[DockerErrorMessage](body, self)
            .flatMap(msg =>
              ZIO.fail(
                CommandFailure.ContainerAlreadyExists(
                  self,
                  containerName.getOrElse(ContainerName.unsafeMake("container name unavailable")),
                  msg
                )
              )
            )
        case statusCode => CommandFailure.unexpectedDockerApiError(body, self, statusCode)
      }
  }

  final case class InspectContainer(containerId: ContainerId) extends Command { self =>

    type Response = InspectContainerResponse

    override def makeResponse(statusCode: Int, body: String) =
      statusCode match {
        case 200        => CommandFailure.decodeResponse[InspectContainerResponse](body, self)
        case 404        => CommandFailure.containerNotFound(self, containerId)
        case statusCode => CommandFailure.unexpectedDockerApiError(body, self, statusCode)
      }
  }

  final case class RemoveContainer(
    containerId: ContainerId,
    force: RemoveContainer.Force.Type,
    volumes: RemoveContainer.Volumes.Type
  ) extends Command { self =>

    type Response = ContainerId

    override def makeResponse(statusCode: Int, body: String) =
      statusCode match {
        case 204        => ZIO.succeed(containerId)
        case 404        => CommandFailure.containerNotFound(self, containerId)
        case statusCode => CommandFailure.unexpectedDockerApiError(body, self, statusCode)
      }
  }

  object RemoveContainer {

    object Force extends Subtype[String] {
      val yes = wrap("true")
      val no  = wrap("false")

      implicit class Syntax(private val force: Force.Type) extends AnyVal {
        def asQueryParam = s"force=${force}"
      }
    }
    type Force = Force.type

    object Volumes extends Subtype[String] {
      val yes = wrap("true")
      val no  = wrap("false")

      implicit class Syntax(val volumes: Volumes.Type) extends AnyVal {
        def asQueryParam = s"volumes=${volumes}"
      }
    }
    type Volumes = Volumes.type

  }

  final case class StartContainer(containerId: ContainerId) extends Command {
    type Response = ContainerId

    override def makeResponse(statusCode: Int, body: String) =
      if (statusCode == 204)
        ZIO.succeed(containerId)
      else
        CommandFailure.unexpectedDockerApiError(body, this, statusCode)
  }

  final case class StopContainer(containerId: ContainerId) extends Command {
    type Response = StopContainer.Result

    override def makeResponse(statusCode: Int, body: String) =
      statusCode match {
        case 204        => ZIO.succeed(StopContainer.Stopped(containerId))
        case 304        => ZIO.succeed(StopContainer.NotRunning(containerId))
        case 404        => CommandFailure.containerNotFound(this, containerId)
        case statusCode => CommandFailure.unexpectedDockerApiError(body, this, statusCode)
      }
  }

  object StopContainer {
    sealed trait Result extends Any

    final case class Stopped(containerId: ContainerId)    extends AnyVal with Result
    final case class NotRunning(containerId: ContainerId) extends AnyVal with Result
  }

}
