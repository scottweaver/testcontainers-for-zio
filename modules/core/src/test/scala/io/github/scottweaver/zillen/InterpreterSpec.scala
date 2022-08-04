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

import zio.test._
import models._
import zio._

object InterpreterSpec extends ZIOSpecDefault {

  val postgresImage = Image("postgres:latest")

  def spec = suite("InterpreterSpec")(
    test("Verify container lifecycle.") {
      val env          = Env.make("POSTGRES_PASSWORD" -> "password")
      val cport        = ProtocolPort.makeTCPPort(5432)
      val exposedPorts = ProtocolPort.Exposed.make(cport)
      val hostConfig   = HostConfig(PortMap.makeOneToOne(cport -> HostInterface.makeUnsafeFromPort(5432)))

      def createImage = Interpreter.run(Command.CreateImage(postgresImage))
      def create(name: ContainerName) =
        Interpreter.run(Command.CreateContainer(env, exposedPorts, hostConfig, postgresImage, Some(name)))
      def running(id: ContainerId) = Docker.readyWhenRunningPromise[Any](id) // ContainerStateCheck.whenRunning[Any](id)
      def start(id: ContainerId)   = Interpreter.run(Command.StartContainer(id))
      def stop(id: ContainerId)    = Interpreter.run(Command.StopContainer(id))
      def exited(id: ContainerId)  = Docker.doneWhenDeadOrExitedPromise[Any](id)
      def remove(id: ContainerId) = Interpreter.run(
        Command.RemoveContainer(id, Command.RemoveContainer.Force.yes, Command.RemoveContainer.Volumes.yes)
      )

      val testCase =
        for {
          name            <- ContainerName.make("zio-postgres-test-container").toZIO
          createImage     <- createImage
          createdResponse <- create(name)
          started         <- start(createdResponse.id)
          running         <- running(createdResponse.id).flatMap(_.await)
          stopping        <- stop(createdResponse.id)
          exited          <- exited(createdResponse.id).flatMap(_.await)
          removed         <- remove(createdResponse.id)
        } yield (createImage, createdResponse, started, running, stopping, exited, removed)

      testCase.map { case (createImage, createdResponse, started, running, stopping, exited, removed) =>
        println(createdResponse)
        assertTrue(
          createImage == postgresImage,
          createdResponse.warnings.isEmpty,
          started == createdResponse.id,
          running,
          stopping == Command.StopContainer.Stopped(createdResponse.id),
          exited,
          removed == createdResponse.id
        )
      }.provide(
        Scope.default,
        ContainerSettings.default[Any](),
        Docker.layer()
      )
    }
  ) @@ TestAspect.sequential @@ TestAspect.withLiveClock

}
