package io.github.scottweaver
package zillen
package models

import zio.json._

final case class CreateContainerResponse(
  @jsonField("Id") id: ContainerId,
  @jsonField("Warnings") warnings: List[String]
)

object CreateContainerResponse {
  implicit val createContainerResponseDecoder: JsonDecoder[CreateContainerResponse] = DeriveJsonDecoder.gen
}
