package io.github.scottweaver.zillen

import zio.test._
import io.github.scottweaver.zillen.models._
import io.netty.bootstrap.Bootstrap
import io.github.scottweaver.zillen.netty.NettyRequest
import zio._

object InterpreterSpec extends ZIOSpecDefault {

  def spec = suite("InterpreterSpec")(
    test("CreateImage receives a 200 response") {
      val testCase =
        Interpreter.run(Command.CreateImage(Image("hello-world:latest")))

      testCase.map { response =>
        println(response)
        assertTrue(response == Image("hello-world:latest"))
      }.provide(
        Scope.default,
        ZLayer.succeed(new Bootstrap),
        NettyRequest.live,
        Interpreter.live
      )

    },
    test("CreateContainer receives a 201 response") {
      val create                  =
        Interpreter.run(Command.CreateContainer(Env.empty, Port.Exposed.empty, Image("hello-world:latest")))
      def start(id: ContainerId)  = Interpreter.run(Command.StartContainer(id))
      def stop(id: ContainerId)   = Interpreter.run(Command.StopContainer(id))
      def remove(id: ContainerId) = Interpreter.run(
        Command.RemoveContainer(id, Command.RemoveContainer.Force.yes, Command.RemoveContainer.Volumes.yes)
      )

      val testCase =
        for {
          createdResponse <- create
          started         <- start(createdResponse.id)
          stopped         <- stop(createdResponse.id)
          removed         <- remove(createdResponse.id)
        } yield (createdResponse, started, stopped, removed)

      testCase.map { case (response, started, stopped, removed) =>
        println(response)
        assertTrue(
          response.warnings.isEmpty,
          started == response.id,
          stopped.isInstanceOf[Command.StopContainer.NotRunning],
          removed == response.id
        )
      }.provide(
        Scope.default,
        ZLayer.succeed(new Bootstrap),
        NettyRequest.live,
        Interpreter.live
      )
    }
  ) @@ TestAspect.sequential

}
