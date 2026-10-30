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

import models._

object ReadyCheckSpec extends ZIOSpecDefault {

  final case class ContainerGoneInterpreter() extends Interpreter {
    def run(command: Command): DockerIO[Any, command.Response] = command match {
      case c @ Command.InspectContainer(containerId) => ZIO.fail(CommandFailure.ContainerNotFound(c, containerId))
      case other                                     => ZIO.die(new IllegalStateException(s"Unexpected command: $other"))
    }
  }

  val spec = suite("ReadyCheckSpec")(
    test("completes the promise with false when the inspect poll itself fails, instead of hanging") {
      val containerId = ContainerId("removed-before-the-poll-ran")
      for {
        promise <- ReadyCheckLive(ContainerGoneInterpreter())
                     .makePromise[Any](containerId, _ => ZIO.succeed(true), ReadyCheck.ContainerRunning(250.millis, 5))
        ready <- promise.await
      } yield assertTrue(!ready)
    }
  ) @@ TestAspect.timeout(10.seconds)
}
