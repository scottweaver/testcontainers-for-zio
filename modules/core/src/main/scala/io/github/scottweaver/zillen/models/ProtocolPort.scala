package io.github.scottweaver
package zillen
package models

import zio.json._
import zio.json.ast.Json

final case class ProtocolPort(portNumber: Int, protocol: Protocol) {
  def asField: String = s"$portNumber/${protocol.asField}"
}

object ProtocolPort {


  implicit val PortEncoder: JsonEncoder[ProtocolPort] =
    JsonEncoder.string.contramap(_.asField)

  implicit val PortDecoder: JsonDecoder[ProtocolPort] =
    JsonDecoder.string.mapOrFail(ProtocolPort.fromString)

  implicit val PortFieldEncoder: JsonFieldEncoder[ProtocolPort] =
    JsonFieldEncoder.string.contramap(_.asField) 

  implicit val PortFieldDecoder: JsonFieldDecoder[ProtocolPort] =
    JsonFieldDecoder.string.mapOrFail(fromString)   

  def makeTCPPort(portNumber: Int): ProtocolPort =
    ProtocolPort(portNumber, Protocol.TCP)

  def makeUDPPort(portNumber: Int): ProtocolPort =
    ProtocolPort(portNumber, Protocol.UDP)

  def makeSCTPPort(portNumber: Int): ProtocolPort =
    ProtocolPort(portNumber, Protocol.SCTP)

  def fromString(portString: String): Either[String, ProtocolPort] = {
    val portSplit = portString.split("/")
    if (portSplit.length != 2) {
      Left(s"Invalid port string format.  Expected `{protocol}/{port number}` but got `$portString` instead.")
    } else {
      val portNumberStr = portSplit(0)
      val portNumber =
        try { Right(portNumberStr.toInt) }
        catch { case t: Throwable => Left(s"Invalid port number, '$portNumberStr'.  Cause: ${t.getLocalizedMessage()}") }
      portNumber.flatMap { portNumber =>
        Protocol
          .fromString(portSplit(1))
          .map(ProtocolPort(portNumber, _))
      }

    }
  }

  final case class Exposed(ports: List[ProtocolPort])

  object Exposed {

    val empty: Exposed = Exposed(List.empty)

    def make(ports: ProtocolPort*): Exposed =
      Exposed(ports.toList)

    private val emptyObj = Json.Obj()

    // The Docker API has a weird way of representing exposed ports e.g `{"80/tcp":{}}`.
    implicit val encoder: JsonEncoder[Exposed] = JsonEncoder.map[String, Json.Obj].contramap[Exposed] { exposed =>
      exposed.ports.map(port => port.asField -> emptyObj).toMap
    }
  }


}
