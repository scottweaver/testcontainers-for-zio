package io.github.scottweaver.zillen.models

import zio.prelude.Newtype

import zio.json._

object Image extends Newtype[String] {

  implicit val encoder: JsonEncoder[Image] = JsonEncoder.string.contramap[Image](unwrap)
  implicit val decoder: JsonDecoder[Image] = JsonDecoder.string.map(wrap)
}
