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
