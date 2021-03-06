package io.github.scottweaver.zillen.models

import zio.prelude.Subtype
import zio._
import HostConfig.PortBinding
import zio.json._
import zio.json.ast._
import io.github.scottweaver.zillen.netty.Network

final case class HostConfig(
  @jsonField("PortBindings") portBindings: Chunk[PortBinding]
)

object HostConfig {

  val empty = HostConfig(Chunk.empty)

  type HostPort = HostPort.Type

  object HostPort extends Subtype[Int] {

    private[zillen] def unsafeMake(i: Int): HostPort =
      wrap(i)

    implicit val hostPortEncoder: JsonEncoder[HostPort] =
      JsonEncoder.string.contramap(_.toString)
  }

  final case class PortBinding(containerPort: Port, hostPorts: NonEmptyChunk[HostPort])

  object PortBinding {
    def make(containerPort: Port, hostPort: HostPort, additionalPorts: HostPort*) =
      PortBinding(containerPort, NonEmptyChunk(hostPort, additionalPorts: _*))

    def makeAutoMapped(containerPort: Port) = {
      Network.findFreePort.map(openPort => PortBinding(containerPort, NonEmptyChunk(HostPort.unsafeMake(openPort))))
    }  
  }

  /**
   * Produces the JSON in the Docker API format:
   * {{{
   *   "PortBindings": {
   *     "22/tcp": [
   *     {
   *       "HostPort": "11022"
   *     }
   * ]
   *   }
   * }}}
   */
  implicit val PortBindingsEncoder: JsonEncoder[Chunk[PortBinding]] =
    JsonEncoder[Json.Obj].contramap[Chunk[PortBinding]] { portBindings =>
      def makeHostPortObj(hostPort: HostPort) = Json.Obj(
        ("HostPort" -> Json.Str(hostPort.toString))
      )

      val tup = portBindings.map { portBinding =>
        val hps = portBinding.hostPorts.map(pb => makeHostPortObj(pb))
        (portBinding.containerPort.asField, Json.Arr(hps))
      }

      Json.Obj(tup)
    }

  implicit val HostConfigEncoder: JsonEncoder[HostConfig] = DeriveJsonEncoder.gen

}
