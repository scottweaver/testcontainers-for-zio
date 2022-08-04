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

trait Network {
  def findOpenPort: DockerIO[Any, Int]
}

object Network {

  val layer = ZLayer.succeed[Network](NetworkLive) // Inference doesn't work as expected when using a case object.

  def findOpenPort = ZIO.serviceWithZIO[Network](_.findOpenPort)
}

case object NetworkLive extends Network {

  def findOpenPort: DockerIO[Any, Int] =
    ZIO
      .acquireRelease(
        ZIO.logDebug(s"Attempting to find an open network port...") *> ZIO.attemptBlocking(new java.net.ServerSocket(0))
      )(socket =>
        ZIO.attemptBlocking(socket.close()).ignoreLogged <* ZIO.logDebug(
          s"Successfully closed socket bound on ${socket.getLocalPort}."
        )
      )
      .map(_.getLocalPort)
      .mapError(Docker.invalidRuntimeState(s"Unable to find an open network port"))
      .tap(p => ZIO.logDebug(s"An open port was found on $p."))
      // Providing the default scope here guarantees that the 'release' function is invoked
      // and the socket is closed immediately after the call to `map` .
      .provide(Scope.default)
}
