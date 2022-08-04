/*
 * Copyright 2021 io.github.scottweaver
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
