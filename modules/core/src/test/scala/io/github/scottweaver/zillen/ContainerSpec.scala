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

package io.github.scottweaver.zillen

import zio._
import zio.test._

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
