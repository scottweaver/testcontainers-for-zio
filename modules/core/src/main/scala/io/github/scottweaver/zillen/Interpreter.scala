package io.github.scottweaver.zillen

import zio._
import io.github.scottweaver.zillen.Command.Pull
import io.github.scottweaver.zillen.netty.NettyRequest
import io.netty.handler.codec.http._
import io.netty.buffer.Unpooled
import io.github.scottweaver.zillen.Command.CreateContainer
import io.github.scottweaver.zillen.models.CreateContainerRequest
import zio.json._
import Interpreter.Response

trait Interpreter {
  def execute(command: Command): ZIO[Any, Throwable, Response]
}

object Interpreter {

  final case class Response(status: Int, body: String)

  def executeCommand(command: Command) = ZIO.serviceWithZIO[Interpreter](_.execute(command))

  val live = ZLayer.fromZIO(ZIO.serviceWith[NettyRequest](InterpreterLive.apply))

}

final case class InterpreterLive(nettyRequest: NettyRequest) extends Interpreter {

  def execute(command: Command): ZIO[Any, Throwable, Response] =
    command match {
      case Pull(image)                               =>
        val uri                  = s"http://localhost/v1.41/images/create?fromImage=${image}"
        val request: HttpRequest = new DefaultFullHttpRequest(
          HttpVersion.HTTP_1_1,
          HttpMethod.POST,
          uri,
          Unpooled.EMPTY_BUFFER
        )
        request.headers().set(HttpHeaderNames.HOST, "daemon")

        nettyRequest.executeRequest(request).map(status => Response(status, ""))
      case CreateContainer(env, exposedPorts, image) =>
        val crequest = CreateContainerRequest(env, exposedPorts, image)
        // val crequest             = CreateContainerRequest(image  )
        println(crequest.toJsonPretty)

        val bb                   = Unpooled.wrappedBuffer(crequest.toJson.getBytes)
        val uri                  = s"http://localhost/v1.41/containers/create"
        println(s"ACTUAL BYTE COUNT: ${crequest.toJson.getBytes.length}")
        println(s"READABLE BYTE COUNT: ${bb.readableBytes()}")
        val request: HttpRequest = new DefaultFullHttpRequest(
          HttpVersion.HTTP_1_1,
          HttpMethod.POST,
          uri,
          bb
        )
        request.headers().set(HttpHeaderNames.HOST, "daemon")
        request.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json")
        request.headers().set(HttpHeaderNames.CONTENT_LENGTH, s"${bb.readableBytes()}")

        nettyRequest.executeRequestWithResponse(request).map { case (status, body) =>
          Response(status, body)
        }

    }

}
