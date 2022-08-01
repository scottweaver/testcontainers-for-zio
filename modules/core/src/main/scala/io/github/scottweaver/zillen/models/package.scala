package io.github.scottweaver.zillen

import zio.json._
import zio.json.ast._
import zio.prelude._
import zio.NonEmptyChunk

package object models {

  type ContainerId = ContainerId.Type

  type ContainerName = ContainerName.Type

  type DockerErrorMessage = DockerErrorMessage.Type

  type Env = Env.Type

  type Image = Image.Type

  type PortMap = PortMap.Type

  def extractValue[A: JsonDecoder](fieldName: String, jsonObj: Json.Obj, default: A): Either[String, A] =
    jsonObj.get(JsonCursor.field(fieldName)).flatMap(_.as[Option[A]].map(_.getOrElse(default)))

  private[zillen] def safeIntFromString(strInt: String, context: String = "Could not convert string to an it.") =
    try { Right(strInt.toInt) }
    catch {
      case t: Throwable =>
        Left(s"$context. Expected valid integer but got '$strInt' instead.  Cause: ${t.getLocalizedMessage()}")
    }

  def validationToEither[A](validation: Validation[String, A]): Either[String, A] = {
    def errorMsg(errors: NonEmptyChunk[String]) = s"""|One or more validation errors occurred:
                                                      |  - ${errors.toList.mkString("\n  - ")}""".stripMargin
    validation.toEither.left.map(errorMsg)
  }

}
