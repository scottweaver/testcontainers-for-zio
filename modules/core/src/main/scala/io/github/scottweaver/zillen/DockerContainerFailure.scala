package io.github.scottweaver.zillen

import io.github.scottweaver.zillen.models._
import zio.json._
import zio._
import io.github.scottweaver.zillen.DockerContainerFailure._

sealed trait DockerContainerFailure { self =>

  def asException: Exception = self match {
    case InvalidDockerRuntimeState(msg, cause)  => new Exception(msg, cause.orNull)
    case InvalidDockerConfiguration(msg, cause) => new Exception(msg, cause.orNull)
    case ContainerReadyCheckFailure(msg, cause) => new Exception(msg, cause.orNull)
    case ContainerReadinessTimeout(msg)         => new Exception(msg)
    case cf: CommandFailure                     => new Exception(cf.toString()) // TODO: This needs to be better.
  }
}

object DockerContainerFailure {

  final case class InvalidDockerRuntimeState(msg: String, cause: Option[Throwable] = None)
      extends DockerContainerFailure

  final case class InvalidDockerConfiguration(msg: String, cause: Option[Throwable] = None)
      extends DockerContainerFailure

  final case class ContainerReadinessTimeout(msg: String) extends DockerContainerFailure

  final case class ContainerReadyCheckFailure(msg: String, cause: Option[Throwable] = None)
      extends DockerContainerFailure

  def fromConfigurationException(context: String, t: Throwable): InvalidDockerConfiguration = {
    val msg = s"${context} Cause: ${t.getMessage}"
    InvalidDockerConfiguration(msg, Some(t))
  }

  def fromConfigurationException(t: Throwable): InvalidDockerConfiguration = {
    fromConfigurationException("Failed to initialize Docker container.", t)
  }

}

sealed trait CommandFailure extends DockerContainerFailure {
  def command: Command
}

object CommandFailure {

  final case class ContainerNotFound(command: Command, containerId: ContainerId) extends CommandFailure

  final case class DockerApiDecodingFailure(body: String, command: Command, reason: String) extends CommandFailure

  final case class ContainerNotReady(command: Command, containerId: ContainerId) extends CommandFailure

  final case class ContainerAlreadyRunning(command: Command, containerId: ContainerId) extends CommandFailure

  final case class ContainerAlreadyExists(command: Command, containerName: ContainerName, message: DockerErrorMessage)
      extends CommandFailure

  final case class ImageNotFound(command: Command, image: Image) extends CommandFailure

  final case class UnexpectedDockerApiError(message: DockerErrorMessage, command: Command, statusCode: Int)
      extends CommandFailure

  final case class UnexpectedHttpError(cause: Throwable, command: Command, uri: String) extends CommandFailure

  def decodeResponse[A: JsonDecoder](body: String, command: Command): DockerIO[Any, A] =
    body.fromJson[A] match {
      case Left(error) =>
        ZIO.debug(s">>> Failed to decode response '${error}'") *> ZIO.fail(
          DockerApiDecodingFailure(body, command, error)
        )
      case Right(a) => ZIO.succeed(a)
    }

  def containerNotFound(command: Command, containerId: ContainerId) =
    ZIO.fail(ContainerNotFound(command, containerId))

  def imageNotFound(command: Command, image: Image) =
    ZIO.fail(ImageNotFound(command, image))

  def nettyRequest[A](
    command: Command,
    response: ZIO[Any, Throwable, (Int, String)],
    uri: String
  ): DockerIO[Any, command.Response] =
    response.mapError { (cause: Throwable) =>
      UnexpectedHttpError(cause, command, uri)
    }.flatMap(r => command.makeResponse(r._1, r._2))

  def nettyRequestNoResponse[A](
    command: Command,
    response: ZIO[Any, Throwable, Int],
    uri: String
  ): DockerIO[Any, command.Response] =
    response.mapError { (cause: Throwable) =>
      UnexpectedHttpError(cause, command, uri)
    }.flatMap(r => command.makeResponse(r, ""))

  def unexpectedDockerApiError(body: String, command: Command, statusCode: Int): DockerIO[Any, command.Response] =
    decodeResponse[DockerErrorMessage](body, command).flatMap { message =>
      ZIO.fail(UnexpectedDockerApiError(message, command, statusCode))
    }
}
