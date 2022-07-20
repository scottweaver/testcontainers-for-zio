package io.github.scottweaver.zillen.models

import zio.prelude.Subtype
import zio.json._

object Image extends Subtype[String] {
  implicit val ImageCodec: JsonCodec[Image.Type] =
    JsonCodec.string.transform(apply(_), identity)
}