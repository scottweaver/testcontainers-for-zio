package io.github.scottweaver.zillen

import zio.test._
import io.github.scottweaver.zillen.models._
import io.netty.bootstrap.Bootstrap
import io.github.scottweaver.zillen.netty.NettyRequest
import zio._

object InterpreterSpec extends ZIOSpecDefault {

  val postgresImage = Image("postgres:latest")

  def spec = suite("InterpreterSpec")(
    test("Verify container lifecycle.") {
      import HostConfig._
      val env          = Env.make("POSTGRES_PASSWORD" -> "password")
      val cport        = Port.makeTCPPort(5432)
      val exposedPorts = Port.Exposed.make(cport)
      val hostConfig   = HostConfig(Chunk(PortBinding.make(cport, HostPort.unsafeMake(5432))))

      def createImage = Interpreter.run(Command.CreateImage(postgresImage))
      def create(name: ContainerName) =
        Interpreter.run(Command.CreateContainer(env, exposedPorts, hostConfig, postgresImage, Some(name)))
      def inspect(id: ContainerId) = ContainerPromise.whenRunning(id)
      def start(id: ContainerId)   = Interpreter.run(Command.StartContainer(id))
      def stop(id: ContainerId)    = Interpreter.run(Command.StopContainer(id))
      def exited(id: ContainerId)  = ContainerPromise.whenDeadOrExited(id)
      def remove(id: ContainerId) = Interpreter.run(
        Command.RemoveContainer(id, Command.RemoveContainer.Force.yes, Command.RemoveContainer.Volumes.yes)
      )

      val testCase =
        for {
          name            <- ContainerName.make("zio-postgres-test-container").toZIO
          createImage     <- createImage
          createdResponse <- create(name)
          started         <- start(createdResponse.id)
          running         <- inspect(createdResponse.id).flatMap(_.await)
          stopping        <- stop(createdResponse.id)
          exited          <- exited(createdResponse.id).flatMap(_.await)
          removed         <- remove(createdResponse.id)
        } yield (createImage, createdResponse, started, running, stopping, exited, removed)

      testCase.map { case (createImage, createdResponse, started, running, stopping, exited, removed) =>
        import State._
        println(createdResponse)
        assertTrue(
          createImage == postgresImage,
          createdResponse.warnings.isEmpty,
          started == createdResponse.id,
          running == Status.Running,
          stopping == Command.StopContainer.Stopped(createdResponse.id),
          exited == Status.Exited,
          removed == createdResponse.id
        )
      }.provide(
        Scope.default,
        ContainerPromise.Settings.default,
        DockerSettings.default,
        ZLayer.succeed(new Bootstrap),
        NettyRequest.live,
        Interpreter.live
      )
    }
  ) @@ TestAspect.sequential @@ TestAspect.withLiveClock

}
