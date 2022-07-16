package io.github.scottweaver.zillen.models

import zio.json._
import zio.json.ast._

final case class ProgressDetail(current: Long, total: Long)

object ProgressDetail {

  implicit val progressDetailDecoder: JsonDecoder[ProgressDetail] =
    JsonDecoder[Json.Obj].mapOrFail { obj =>
      for {
        current  <- extractValue("current", obj, 0L)
        total    <- extractValue("total", obj, 0L)
      } yield ProgressDetail(current, total)
    }
}
