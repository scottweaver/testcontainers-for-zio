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
import zio.prelude._

final case class DockerSettings(
  socketPath: DockerSocketPath
  // inspectContainerPromiseSettings: InspectContainerPromise.Settings,
  // readyCheckSettings: ReadyCheck.Settings
) { self =>

  // def withInspectContainerPromiseSettings(
  //   inspectContainerPromiseSettings: InspectContainerPromise.Settings
  // ): DockerSettings =
  //   copy(inspectContainerPromiseSettings = inspectContainerPromiseSettings)

  // def withReadyCheckSettings(
  //   readyCheckSettings: ReadyCheck.Settings
  // ): DockerSettings =
  //   copy(readyCheckSettings = readyCheckSettings)

  def withSocketPath(socketPath: DockerSocketPath): DockerSettings =
    copy(socketPath = socketPath)

}

object DockerSettings {

  object DockerSocketPath extends Subtype[String]

  val socketPath = ZIO.serviceWith[DockerSettings](_.socketPath)

  def default(
    builder: DockerSettings => DockerSettings = identity
  ) =
    ZLayer.fromZIO {
      val defaultPath = "/var/run/docker.sock"

      for {
        _ <- validFilePath(defaultPath).mapError(_.asException).orDie
      } yield {
        builder(DockerSettings(DockerSocketPath(defaultPath)))
      }
    }

}
