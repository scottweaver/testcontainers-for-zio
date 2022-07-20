package io.github.scottweaver.zillen.models

import zio.prelude._
import zio.json._
import zio.json.ast.Json

object DockerErrorMessage extends Subtype[String] {

  implicit val DockerErrorMessageDecoder: JsonDecoder[DockerErrorMessage.Type] =
    JsonDecoder[Json.Obj].mapOrFail { obj =>
      obj.fields.find(_._1 == "message") match {
        case Some((_, message)) => message.as[String].map(DockerErrorMessage(_))
        case None               => Left(s"Could not deserialize error message.  Missing 'message' field.  Raw message: ${obj.toJson}")
      }

    }
}
