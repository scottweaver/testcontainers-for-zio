package io.github.scottweaver.zillen.models

import zio.json._

final case class CreateContainerRequest(
  @jsonField("Env") env: Env,
  @jsonField("ExposedPorts") exposedPorts: Port.Exposed,
  @jsonField("Image") image: Image
)

object CreateContainerRequest {
  implicit val encoder: JsonEncoder[CreateContainerRequest] = DeriveJsonEncoder.gen
}
