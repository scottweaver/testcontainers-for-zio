package io.github.scottweaver.zillen.models

import Port.Protocol
import zio.json._
import zio.json.ast.Json

final case class Port(portNumber: Int, protocol: Protocol) {
  def asField: String = s"$portNumber/${protocol.asField}"
}

object Port {

  def makeTCPPort(portNumber: Int): Port =
    Port(portNumber, Protocol.TCP)

  def makeUDPPort(portNumber: Int): Port =
    Port(portNumber, Protocol.UDP)

  def makeSCTPPort(portNumber: Int): Port =
    Port(portNumber, Protocol.SCTP)

  final case class Exposed(ports: List[Port])

  object Exposed {

    val empty: Exposed = Exposed(List.empty)

    def make(ports: Port*): Exposed =
      Exposed(ports.toList)

    private val emptyObj = Json.Obj()

    // The Docker API has a weird way of representing exposed ports e.g `{"80/tcp":{}}`.
    implicit val encoder: JsonEncoder[Exposed] = JsonEncoder.map[String, Json.Obj].contramap[Exposed] { exposed =>
      exposed.ports.map(port => port.asField -> emptyObj).toMap
    }
  }

  sealed trait Protocol { def asField: String }

  object Protocol {
    case object TCP  extends Protocol { val asField = "tcp"  }
    case object UDP  extends Protocol { val asField = "udp"  }
    case object SCTP extends Protocol { val asField = "sctp" }

  }

}
