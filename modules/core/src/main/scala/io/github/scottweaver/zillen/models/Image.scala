package io.github.scottweaver.zillen.models

import zio.prelude.Subtype

object Image extends Subtype[String] with SubtypeJsonCodec[String]
