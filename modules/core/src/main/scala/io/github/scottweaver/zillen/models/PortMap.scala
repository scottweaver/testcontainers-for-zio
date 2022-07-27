package io.github.scottweaver.zillen.models

import zio.prelude._
import zio._
import zio.json._
import io.github.scottweaver.zillen._

object PortMap extends Subtype[Map[ProtocolPort, NonEmptyChunk[HostInterface]]] {

  val empty: PortMap = wrap(Map.empty)

  def make(portMaps: (ProtocolPort, NonEmptyChunk[HostInterface])*): PortMap =
    wrap(Map(portMaps: _*))

  def makeOneToOne(portMaps: (ProtocolPort, HostInterface)*): PortMap =
    make(portMaps.map { case (port, hostPort) => (port, NonEmptyChunk(hostPort)) }: _*)

  def makeAutoMapped(containerPorts: ProtocolPort*): RIO[Network, PortMap] = {
    val withHostPort = ZIO
      .foreach(containerPorts)(port =>
        Network.findOpenPort.map(openPort =>
          port -> NonEmptyChunk(HostInterface.makePortOnly(HostInterface.HostPort.unsafeMake(openPort)))
        )
      )
    withHostPort.map(make)
  }

  def makeFromExposedPorts(exposedPorts: ProtocolPort.Exposed): RIO[Network, PortMap] =
    makeAutoMapped(exposedPorts.ports:_*)

  implicit val PortMapCodec: JsonCodec[PortMap] =
    JsonCodec.map[ProtocolPort, NonEmptyChunk[HostInterface]].transform(wrap, unwrap)
  
  implicit class Syntax(portMap: PortMap) {

    def findExternalHostPort(port: Int, protocol: Protocol): Option[HostInterface] = {
      portMap.get(ProtocolPort(port, protocol)).map(_.head)
    }
  }

}
