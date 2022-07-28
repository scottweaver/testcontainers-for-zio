package io.github.scottweaver
package zillen
package models

import zio.prelude._
import zio.json._

object ContainerId extends Subtype[String] {
  implicit val ContainerIdCodec: JsonCodec[ContainerId.Type] =
    JsonCodec.string.transform(apply(_), identity)
}
