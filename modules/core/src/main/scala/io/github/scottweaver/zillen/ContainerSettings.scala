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

final case class ContainerSettings[A](
  inspectContainerPromiseSettings: ReadyCheck.ContainerRunning,
  readyCheckSettings: ReadyCheck.ContainerReady
) { self =>

  def withInspectContainerPromiseSettings(
    inspectContainerPromiseSettings: ReadyCheck.ContainerRunning
  ): ContainerSettings[A] =
    copy(inspectContainerPromiseSettings = inspectContainerPromiseSettings)

  def withReadyCheckSettings(
    readyCheckSettings: ReadyCheck.ContainerReady
  ): ContainerSettings[A] =
    copy(readyCheckSettings = readyCheckSettings)

  def as[B]: ContainerSettings[B] = self.asInstanceOf[ContainerSettings[B]]
}

object ContainerSettings {
  val defaultPromisedSettings   = ReadyCheck.ContainerRunning(250.millis, 5)
  val defaultReadyCheckSettings = ReadyCheck.ContainerReady(250.millis, 5)

  def default[A: Tag](builder: ContainerSettings[A] => ContainerSettings[A] = identity[ContainerSettings[A]] _) =
    ZLayer.succeed {
      builder(
        ContainerSettings[A](
          defaultPromisedSettings,
          defaultReadyCheckSettings
        )
      )
    }
}
