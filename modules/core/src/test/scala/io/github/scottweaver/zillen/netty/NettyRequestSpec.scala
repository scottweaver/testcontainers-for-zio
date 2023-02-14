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

package io.github.scottweaver
package zillen
package netty

import io.netty.bootstrap.Bootstrap
import io.netty.buffer.Unpooled
import io.netty.handler.codec.http._
import zio._
import zio.test._

import zillen._

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

        val testCase = NettyRequestHandler.executeRequest(request)

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

        val testCase = NettyRequestHandler.executeRequestWithResponse(request)

        testCase.map { case (statusCode, body) =>
          println(s">>> BODY: ${body}")
          assertTrue(statusCode == 200)
        }
      } @@ TestAspect.ignore
    )
      .provideShared(
        ZLayer.succeed(new Bootstrap),
        DockerSettings.default(),
        Scope.default,
        NettyRequestHandler.layer
      )

}
