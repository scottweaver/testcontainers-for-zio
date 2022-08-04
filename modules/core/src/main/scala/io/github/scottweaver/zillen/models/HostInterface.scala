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

import zio.json._
import zio.prelude.Newtype

final case class HostInterface(
  @jsonField("HostIp") hostIp: Option[String],
  @jsonField("HostPort") hostPort: HostInterface.HostPort
) {

  val hostAddress: String = s"${hostIp.filter(_.nonEmpty).getOrElse("localhost")}:${hostPort}"
}

object HostInterface {

  type HostPort = HostPort.Type

  object HostPort extends Newtype[Int] {

    private[zillen] def unsafeMake(i: Int): HostPort =
      wrap(i)

    implicit val hostPortCodec: JsonCodec[HostPort] =
      JsonCodec.string.transformOrFail(s => safeIntFromString(s, s"Invalid host port").map(wrap), unwrap(_).toString)

  }

  def fromPortProtocol(pp: ProtocolPort): HostInterface =
    HostInterface(None, HostPort.unsafeMake(pp.portNumber))

  def makePortOnly(hostPort: HostPort): HostInterface =
    HostInterface(None, hostPort)

  private[zillen] def makeUnsafeFromPort(hostPort: Int): HostInterface =
    HostInterface(None, HostPort.unsafeMake(hostPort))

  implicit val hostInterfaceCode: JsonCodec[HostInterface] = DeriveJsonCodec.gen[HostInterface]
}
