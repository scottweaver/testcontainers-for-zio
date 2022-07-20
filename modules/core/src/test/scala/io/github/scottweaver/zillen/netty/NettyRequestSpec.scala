package io.github.scottweaver.zillen.netty

import zio.test._
import zio._
import io.netty.handler.codec.http._
import io.netty.buffer.Unpooled
import io.netty.bootstrap.Bootstrap
import io.github.scottweaver.zillen.DockerSettings

object NettyRequestSpec extends ZIOSpecDefault {
  def spec =
    suite("NettyRequestSpec")(
      test("test") {
        val request: HttpRequest = new DefaultFullHttpRequest(
          HttpVersion.HTTP_1_1,
          HttpMethod.POST,
          "http://localhost/v1.41/images/create?fromImage=hello-world:latest",
          // HttpMethod.GET,
          // "http://localhost/v1.41/images/json",
          Unpooled.EMPTY_BUFFER
        )
        request.headers().set(HttpHeaderNames.HOST, "daemon")

        val testCase = NettyRequest.executeRequest(request)

        testCase.map { statusCode =>
          assertTrue(statusCode == 200)
        }
      },
      test("Inspect container") {

        val containerId = "fbceb9e25093f72f00362b0e519fc1b9f5c10d4551c3d5687b70e72f43f241cf"

        val request: HttpRequest = new DefaultFullHttpRequest(
          HttpVersion.HTTP_1_1,
          HttpMethod.GET,
          s"http://localhost/v1.41/containers/${containerId}/json",
          Unpooled.EMPTY_BUFFER
        )
        request.headers().set(HttpHeaderNames.HOST, "daemon")

        val testCase = NettyRequest.executeRequestWithResponse(request)

        testCase.map { case (statusCode, body) =>
          println(s">>> BODY: ${body}")
          assertTrue(statusCode == 200)
        }
      }
    )
      .provideShared(
        ZLayer.succeed(new Bootstrap),
        DockerSettings.default,
        Scope.default,
        NettyRequest.live
      )

}
