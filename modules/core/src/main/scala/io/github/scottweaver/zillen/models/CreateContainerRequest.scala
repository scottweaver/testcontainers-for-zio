package io.github.scottweaver
package zillen
package models

import zio.json._

final case class CreateContainerRequest(
  @jsonField("Env") env: Env,
  @jsonField("ExposedPorts") exposedPorts: ProtocolPort.Exposed,
  @jsonField("HostConfig") hostConfig: HostConfig,
  @jsonField("Image") image: Image
)

object CreateContainerRequest {
  implicit val encoder: JsonEncoder[CreateContainerRequest] = DeriveJsonEncoder.gen
}
