package io.github.scottweaver.zillen.models

import zio.json._

final case class InspectContainerResponse(
  @jsonField("HostConfig") hostConfig: HostConfig,
  @jsonField("Name") name: Option[ContainerName],
  @jsonField("NetworkSettings") networkSettings: NetworkSettings,
  @jsonField("State") state: State,
)

object InspectContainerResponse {
  implicit val inspectContainerResponseJson: JsonDecoder[InspectContainerResponse] =
    DeriveJsonDecoder.gen
}
