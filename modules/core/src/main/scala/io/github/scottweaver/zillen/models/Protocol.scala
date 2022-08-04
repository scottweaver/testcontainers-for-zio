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

sealed trait Protocol { def asField: String }

object Protocol {

  def fromString(protocol: String): Either[String, Protocol] = protocol match {
    case "tcp"  => Right(TCP)
    case "udp"  => Right(UDP)
    case "sctp" => Right(SCTP)
    case _      => Left(s"Invalid protocol.  Expected `tcp`, `udp`, or `sctp` but got `$protocol` instead.")
  }
  case object TCP  extends Protocol { val asField = "tcp"  }
  case object UDP  extends Protocol { val asField = "udp"  }
  case object SCTP extends Protocol { val asField = "sctp" }

  implicit val ProtocolCodec: JsonCodec[Protocol] =
    JsonCodec.string.transformOrFail(fromString, _.asField)
}
