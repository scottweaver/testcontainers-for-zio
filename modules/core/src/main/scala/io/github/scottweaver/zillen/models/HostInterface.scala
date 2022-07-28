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
