package io.github.scottweaver.zillen.models

import zio.prelude._

object ContainerId extends Subtype[String] with SubtypeJsonCodec[String]
