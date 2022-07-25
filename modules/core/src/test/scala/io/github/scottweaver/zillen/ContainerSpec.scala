package io.github.scottweaver.zillen

import zio.test._
import zio._
import io.github.scottweaver.zillen.models._
import io.github.scottweaver.zillen.netty.NettyRequest
import io.netty.bootstrap.Bootstrap

object ContainerSpec extends ZIOSpecDefault {

  val postgresImage   = Image("postgres:latest")
  val name            = ContainerName.unsafeMake("zio-postgres-test-container")
  val env             = Env.make("POSTGRES_PASSWORD" -> "password")
  val cport           = ProtocolPort.makeTCPPort(5432)
  val exposedPorts    = ProtocolPort.Exposed.make(cport)
  val hostConfig      = HostConfig(PortMap.makeOneToOne(cport -> HostInterface.makeUnsafeFromPort(5432)))
  val createContainer = Command.CreateContainer(env, exposedPorts, hostConfig, postgresImage, Some(name))

  val spec = suite("ContainerSpec")(
    test("#scopedContainer should properly run the entire lifecycle of a container.") {

      val testCase = for {
        createAndInspect <- Container.makeScopedContainer(createContainer)
        (create, promise) = createAndInspect
        inspectAndStatus <- promise.await
        (inspect, status) = inspectAndStatus

      } yield (create, inspect, status)

      testCase.map { case (_, _, status) =>
        assertTrue(
          status == State.Status.Running
        )
      }

    }
  ).provide(
    Scope.default,
    ZLayer.succeed(new Bootstrap),
    NettyRequest.live,
    DockerSettings.default,
    InspectContainerPromise.Settings.default,
    Interpreter.live
  ) @@ TestAspect.withLiveClock
}
