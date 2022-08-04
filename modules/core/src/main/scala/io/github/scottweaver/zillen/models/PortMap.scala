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
package models

import io.github.scottweaver.zillen._
import zio._
import zio.json._
import zio.prelude._

object PortMap extends Subtype[Map[ProtocolPort, NonEmptyChunk[HostInterface]]] {

  val empty: PortMap = wrap(Map.empty)

  def make(portMaps: (ProtocolPort, NonEmptyChunk[HostInterface])*): PortMap =
    wrap(Map(portMaps: _*))

  def makeOneToOne(portMaps: (ProtocolPort, HostInterface)*): PortMap =
    make(portMaps.map { case (port, hostPort) => (port, NonEmptyChunk(hostPort)) }: _*)

  def makeAutoMapped(containerPorts: ProtocolPort*): DockerIO[Network, PortMap] = {
    val withHostPort = ZIO
      .foreach(containerPorts)(port =>
        Network.findOpenPort.map(openPort =>
          port -> NonEmptyChunk(HostInterface.makePortOnly(HostInterface.HostPort.unsafeMake(openPort)))
        )
      )
    withHostPort.map(mapped => make(mapped.toSeq: _*))
  }

  def makeFromExposedPorts(exposedPorts: ProtocolPort.Exposed): DockerIO[Network, PortMap] =
    makeAutoMapped(exposedPorts.ports: _*)

  implicit val PortMapCodec: JsonCodec[PortMap] =
    JsonCodec.map[ProtocolPort, NonEmptyChunk[HostInterface]].transform(wrap, unwrap)

  implicit class Syntax(portMap: PortMap) {

    def findExternalHostPort(port: Int, protocol: Protocol): Option[HostInterface] =
      portMap.get(ProtocolPort(port, protocol)).map(_.head)
  }

}
