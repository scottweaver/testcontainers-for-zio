package io.github.scottweaver.zillen

import zio._
import io.github.scottweaver.zillen.netty.NettyRequest
import io.netty.handler.codec.http._
import io.netty.buffer.Unpooled
import io.github.scottweaver.zillen.models.CreateContainerRequest
import zio.json._
import io.github.scottweaver.zillen.Command._
import io.netty.buffer.ByteBuf
import io.github.scottweaver.zillen.Command.StopContainer.NotRunning
import io.github.scottweaver.zillen.Command.StopContainer.Stopped

trait Interpreter {
  def run(command: Command): DockerIO[Any, command.Response]
}

object Interpreter {

  val live = ZLayer.fromZIO(ZIO.serviceWith[NettyRequest](InterpreterLive.apply))

  def run(command: Command) =
    ZIO.serviceWithZIO[Interpreter](_.run(command))
  // def run[A](command: Command)(implicit ev0: A =:= command.Response): DockerIO[Interpreter, A] =
  //   ZIO.serviceWithZIO[Interpreter](_.run(command).map(_.asInstanceOf[A]))
}

final case class InterpreterLive(nettyRequest: NettyRequest) extends Interpreter {

  private def makeRequest(uri: String, method: HttpMethod, byteBuf: Option[ByteBuf]) = {
    val request: HttpRequest = new DefaultFullHttpRequest(
      HttpVersion.HTTP_1_1,
      method,
      uri,
      byteBuf.getOrElse(Unpooled.EMPTY_BUFFER)
    )
    request.headers().set(HttpHeaderNames.HOST, "daemon")
    byteBuf.map(byteBuf => request.headers().set(HttpHeaderNames.CONTENT_LENGTH, s"${byteBuf.readableBytes()}"))

    request
  }

  private def makeDELETE(uri: String) = makeRequest(uri, HttpMethod.DELETE, None)

  private def makeGET(uri: String) =
    makeRequest(uri, HttpMethod.GET, None)

  private def makePOST[A: JsonEncoder](uri: String, requestBody: A): HttpRequest = {
    val json    = requestBody.toJson
    val bytes   = json.toString.getBytes("UTF-8")
    val byteBuf = Unpooled.wrappedBuffer(bytes)
    val request = makeRequest(uri, HttpMethod.POST, Some(byteBuf))
    request.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json")
    request
  }

  private def makePOST(uri: String): HttpRequest =
    makeRequest(uri, HttpMethod.POST, None)

  def run(command: Command): DockerIO[Any, command.Response] = {

    def widen[C <: Command { type Response = R0 }, R0](c: => DockerIO[Any, R0]): DockerIO[Any, command.Response] =
      c.asInstanceOf[DockerIO[Any, command.Response]]

    widen {
      command match {
        case c @ CreateImage(image) =>
          val uri     = s"http://localhost/v1.41/images/create?fromImage=${image}"
          val request = makePOST(uri)
          CommandFailure
            .nettyRequestNoResponse(c, nettyRequest.executeRequest(request), uri)
        case c @ InspectContainer(containerId) =>
          val uri     = s"http://localhost/v1.41/containers/${containerId}/json"
          val request = makeGET(uri)
          CommandFailure
            .nettyRequest(c, nettyRequest.executeRequestWithResponse(request), uri)
        case c @ CreateContainer(env, exposedPorts, hostConfig, image, name) =>
          val nameQuery = name.map(n => s"?name=$n").getOrElse("")
          val uri       = s"http://localhost/v1.41/containers/create${nameQuery}"
          val request   = makePOST(uri, CreateContainerRequest(env, exposedPorts, hostConfig, image))
          CommandFailure
            .nettyRequest(c, nettyRequest.executeRequestWithResponse(request), uri)
        case c @ RemoveContainer(containerId, force, volumes) =>
          val uri     = s"http://localhost/v1.41/containers/${containerId}?${force.asQueryParam}&${volumes.asQueryParam}"
          val request = makeDELETE(uri)
          CommandFailure
            .nettyRequest(c, nettyRequest.executeRequestWithResponse(request), uri)
        case c @ StartContainer(containerId) =>
          val uri     = s"http://localhost/v1.41/containers/${containerId}/start"
          val request = makePOST(uri)
          ZIO.logInfo(s"Attempting to start container with id '${containerId}'.") *>
            CommandFailure
              .nettyRequest(c, nettyRequest.executeRequestWithResponse(request), uri)
        case c @ StopContainer(containerId) =>
          val uri     = s"http://localhost/v1.41/containers/${containerId}/stop"
          val request = makePOST(uri)
          ZIO.logInfo(s"Attempting to stop container with id '${containerId}'.") *>
            CommandFailure
              .nettyRequest(c, nettyRequest.executeRequestWithResponse(request), uri)
              .tap {
                case NotRunning(containerId) => ZIO.logInfo(s"Container '${containerId}' was not running.")
                case Stopped(containerId)    => ZIO.logInfo(s"Successfully stopped container '${containerId}'.")
              }
      }

    }
  }

}
