package io.github.scottweaver
package zillen
package models

import zio.json._

final case class Port(
    @jsonField("PrivatePort") privatePort: Int,
    @jsonField("PublicPort") publicPort: Int,
    @jsonField("Type") protocol: Protocol
)

object Port {
    implicit val json: JsonCodec[Port] = DeriveJsonCodec.gen[Port]
}
