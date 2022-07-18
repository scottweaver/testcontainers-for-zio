package io.github.scottweaver.zillen.models

import zio.prelude._
import zio.json._

trait SubtypeJsonCodec[A] { self: Subtype[A] =>

  implicit def subtypeDecoder(implicit ev0: JsonDecoder[A]): JsonDecoder[self.Type] = JsonDecoder[A].map(self.wrap)
  implicit def subtypeEncoder(implicit ev0: JsonEncoder[A]): JsonEncoder[self.Type] =
    JsonEncoder[A].contramap(self.unwrap)

}
