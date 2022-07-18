package io.github.scottweaver.zillen.models

import zio.prelude._
import zio.json._

final case class CreateImageResponse(
  id: Option[CreateImageResponse.Id],
  progress: Option[String],
  progressDetail: Option[ProgressDetail],
  status: String
)

object CreateImageResponse {

  type Id = Id.Type

  object Id extends Subtype[String] {

    implicit val IdDecoder: JsonDecoder[Id] = JsonDecoder.string.map(wrap(_))
  }

  implicit val CreateImageResponseDecoder: JsonDecoder[CreateImageResponse] = DeriveJsonDecoder.gen[CreateImageResponse]

}
