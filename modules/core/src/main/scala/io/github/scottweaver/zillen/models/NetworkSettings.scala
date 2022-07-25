package io.github.scottweaver.zillen.models

import zio.json._

final case class NetworkSettings(
  @jsonField("Ports") ports: PortMap
)

object NetworkSettings {
    implicit val networkSettingsCodec: JsonCodec[NetworkSettings] =
      DeriveJsonCodec.gen[NetworkSettings]
}
