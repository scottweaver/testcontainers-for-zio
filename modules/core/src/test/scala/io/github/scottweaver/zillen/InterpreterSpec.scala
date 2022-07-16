package io.github.scottweaver.zillen

import zio.test._
import io.github.scottweaver.zillen.models._
import io.netty.bootstrap.Bootstrap
import io.github.scottweaver.zillen.netty.NettyRequest
import zio._

object InterpreterSpec extends ZIOSpecDefault {

  def spec = suite("InterpreterSpec")(
    test("PullImage receives a 200 response") {
      val testCase =
        Interpreter.executeCommand(Command.Pull(Image("alpine:latest")))

      testCase.map { response =>
        println(response)
        assertTrue(response.status == 200)
      }.provide(
        Scope.default,
        ZLayer.succeed(new Bootstrap),
        NettyRequest.live,
        Interpreter.live
      )

    },
    test("CreateContainer receives a 201 response") {
      val testCase =
        Interpreter.executeCommand(Command.CreateContainer(Env.empty, Port.Exposed.empty, Image("alpine:latest")))

      testCase.map { response =>
        println(response)
        assertTrue(response.status == 201)
      }.provide(
        Scope.default,
        ZLayer.succeed(new Bootstrap),
        NettyRequest.live,
        Interpreter.live
      )
    }
  ) @@ TestAspect.sequential

}
