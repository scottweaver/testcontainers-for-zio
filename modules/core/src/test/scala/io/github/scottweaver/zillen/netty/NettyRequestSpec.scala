package io.github.scottweaver.zillen.netty

import zio.test._
import zio._
import io.netty.handler.codec.http._
import io.netty.buffer.Unpooled
import io.netty.bootstrap.Bootstrap

object NettyRequestSpec extends ZIOSpecDefault {
  def spec =
    suite("NettyRequestSpec")(
      test("test") {
        val request: HttpRequest = new DefaultFullHttpRequest(
          HttpVersion.HTTP_1_1,
          HttpMethod.POST,
          "http://localhost/v1.41/images/create?fromImage=alpine",
          // HttpMethod.GET,
          // "http://localhost/v1.41/images/json",
          Unpooled.EMPTY_BUFFER
        )
        request.headers().set(HttpHeaderNames.HOST, "daemon")

        val testCase = NettyRequest.executeRequest(request) <* ZIO.sleep(2.seconds)

        testCase.map { statusCode =>
          assertTrue(statusCode == 200)
        }
      }
    )
      .provideShared(
        ZLayer.succeed(new Bootstrap),
        Scope.default,
        NettyRequest.live
      ) @@ TestAspect.timeout(10.seconds) @@ TestAspect.withLiveClock

}
