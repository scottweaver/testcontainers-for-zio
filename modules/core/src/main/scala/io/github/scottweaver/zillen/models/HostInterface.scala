package io.github.scottweaver.zillen.models

import zio.json._
import zio.prelude.Newtype

final case class HostInterface(
  @jsonField("HostIp") hostIp: Option[String],
  @jsonField("HostPort") hostPort: HostInterface.HostPort
)

object HostInterface {

  type HostPort = HostPort.Type

  object HostPort extends Newtype[Int] {

    private[zillen] def unsafeMake(i: Int): HostPort =
      wrap(i)

    implicit val hostPortCodec: JsonCodec[HostPort] =
      JsonCodec.string.transformOrFail(s => safeIntFromString(s, s"Invalid host port").map(wrap), unwrap(_).toString)

  }

  def makePortOnly(hostPort: HostPort): HostInterface =
    HostInterface(None, hostPort)

  private[zillen] def makeUnsafeFromPort(hostPort: Int): HostInterface =
    HostInterface(None, HostPort.unsafeMake(hostPort))

  implicit val hostInterfaceCode: JsonCodec[HostInterface] = DeriveJsonCodec.gen[HostInterface]
}
