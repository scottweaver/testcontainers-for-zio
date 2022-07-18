package io.github.scottweaver.zillen

import zio.json._
import zio.json.ast._

package object models {

  type ContainerId = ContainerId.Type

  type Env = Env.Type

  type Image = Image.Type

  def extractValue[A: JsonDecoder](fieldName: String, jsonObj: Json.Obj, default: A): Either[String, A] =
    jsonObj.get(JsonCursor.field(fieldName)).flatMap(_.as[Option[A]].map(_.getOrElse(default)))

}
