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

package io.github.scottweaver
package zillen
package models

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

    def safeMake(id: String) = wrap(id)

    implicit val IdDecoder: JsonDecoder[Id] = JsonDecoder.string.map(wrap(_))
  }

  implicit val CreateImageResponseDecoder: JsonDecoder[CreateImageResponse] = DeriveJsonDecoder.gen[CreateImageResponse]

}
