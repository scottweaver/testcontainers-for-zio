package io.github.scottweaver.zillen

import zio.test._
import io.github.scottweaver.zillen.models._
import io.netty.bootstrap.Bootstrap
import io.github.scottweaver.zillen.netty.NettyRequest
import zio._

object InterpreterSpec extends ZIOSpecDefault {

  val postgresImage = Image("postgres:latest")

  def spec = suite("InterpreterSpec")(
    test("CreateImage receives a 200 response") {
      val testCase =
        Interpreter.run(Command.CreateImage(postgresImage))

      testCase.map { response =>
        println(response)
        assertTrue(response == postgresImage)
      }.provide(
        Scope.default,
        ZLayer.succeed(new Bootstrap),
        NettyRequest.live,
        Interpreter.live
      )

    },
    test("CreateContainer receives a 201 response") {
      import HostConfig._
      val env          = Env.make("POSTGRES_PASSWORD" -> "password")
      val cport        = Port.makeTCPPort(5432)
      val exposedPorts = Port.Exposed.make(cport)
      val hostConfig   = HostConfig(Chunk(PortBinding(cport, HostPort(5432))))

      val create                  =
        Interpreter.run(Command.CreateContainer(env, exposedPorts, hostConfig, postgresImage))
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
      // } yield (createdResponse, started)

      testCase.map { case (response, started, stopped, removed) =>
        // testCase.map { case (response, started) =>
        println(response)
        assertTrue(
          response.warnings.isEmpty,
          started == response.id,
          stopped.isInstanceOf[Command.StopContainer.Stopped],
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
