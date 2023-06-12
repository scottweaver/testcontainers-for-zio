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

import io.github.scottweaver.zillen.DockerContainerFailure._
import io.github.scottweaver.zillen.models._
import zio._
import zio.json._

sealed trait DockerContainerFailure { self =>

  def asException: DockerContainerFailureException = DockerContainerFailureException(self)
}

object DockerContainerFailure {

  final case class InvalidDockerRuntimeState(msg: String, cause: Option[Throwable] = None)
      extends DockerContainerFailure

  final case class InvalidDockerConfiguration(msg: String, cause: Option[Throwable] = None)
      extends DockerContainerFailure

  final case class ContainerReadinessTimeout(msg: String) extends DockerContainerFailure

  final case class ContainerReadyCheckFailure(msg: String, cause: Option[Throwable] = None)
      extends DockerContainerFailure

  final case class DockerContainerFailureException(failure: DockerContainerFailure) extends Exception()

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
        ZIO.debug(s"Failed to decode response '${error}'") *> ZIO.fail(
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

  def unexpectedDockerApiError(body: String, command: Command, statusCode: Int): DockerIO[Any, command.Response] = {
    import DockerErrorMessage.DockerErrorMessageDecoder
    decodeResponse[DockerErrorMessage](body, command).flatMap { message =>
      ZIO.fail(UnexpectedDockerApiError(message, command, statusCode))
    }
  }
}
