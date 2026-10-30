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

package io.github.scottweaver
package zillen

import zio._

abstract class ContainerBootstrap[RI, RO, ME <: ContainerBootstrap[RI, RO, ME]](
  containerName: Option[ContainerName],
  exposedPorts: ExposedPorts,
  makeEnv: DockerIO[RI, Env],
  makeImage: DockerIO[RI, Image]
) { self =>

  def readyCheck(container: Container): RIO[RI, Boolean]

  def makeZEnvironment(container: Container): ZIO[RI, Nothing, ZEnvironment[RO]]

  def makeCommand =
    for {
      image   <- makeImage
      env     <- makeEnv
      portMap <- Docker.automapExposedPorts(exposedPorts)
    } yield Docker.cmd.createContainer(
      env = env,
      exposedPorts = exposedPorts,
      hostConfig = Docker.makeHostConfig(portMap),
      image = image,
      containerName = containerName
    )

  type RI0 = RI with ReadyCheck with Interpreter with Network with ContainerSettings[ME] with Scope

  def layer(implicit ev2: Tag[ME]): ZLayer[RI0, Nothing, RO] = ZLayer.fromZIOEnvironment {

    def requireReady(ready: Boolean, what: String, id: ContainerId) =
      ZIO.unless(ready)(
        Docker.failReadinessTimeout(
          s"Container '$id' did not become $what within the configured retries."
        )
      )

    val out = for {
      containerAndPromise             <- makeCommand.flatMap(Docker.makeScopedContainer[ME])
      (createResponse, runningPromise) = containerAndPromise
      running                         <- runningPromise.await
      _                               <- requireReady(running, "running", createResponse.id)
      container                       <- Docker.inspectContainer(createResponse.id)
      readyPromise                    <- Docker.makeReadyCheckPromise[RI, ME](createResponse.id, readyCheck)
      ready                           <- readyPromise.await
      _                               <- requireReady(ready, "ready", createResponse.id)
    } yield makeZEnvironment(container)

    out.mapError(_.asException).flatten.orDie
  }

}
