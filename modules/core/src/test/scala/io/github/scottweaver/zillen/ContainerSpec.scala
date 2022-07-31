package io.github.scottweaver.zillen

import zio.test._
import zio._

object ContainerSpec extends ZIOSpecDefault {

  val postgresImage   = Docker.makeImage("postgres:latest").toOption.get
  val name            = Docker.makeContainerName("zio-postgres-test-container").toOption.get
  val env             = Docker.makeEnv("POSTGRES_PASSWORD" -> "password")
  val exposedPorts    = Docker.makeExposedTCPPorts(5432)
  val hostConfig      = Docker.makeHostConfig(Docker.mirrorExposedPorts(exposedPorts))
  val createContainer = Docker.cmd.createContainer(env, exposedPorts, hostConfig, postgresImage, Some(name))

  val spec = suite("ContainerSpec")(
    test("#scopedContainer should properly run the entire lifecycle of a container.") {

      val testCase = for {
        scopedContainer         <- Docker.makeScopedContainer[Any](createContainer)
        (create, runningPromise) = scopedContainer
        running                 <- runningPromise.await

      } yield running

      testCase.map { status =>
        assertTrue(status)
      }
    }
  ).provide(
    Scope.default,
    ContainerSettings.default[Any](),
    Docker.layer()
  ) @@ TestAspect.withLiveClock
}
