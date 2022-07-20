package io.github.scottweaver.zillen.models

import zio.prelude._
import zio.json._

object ContainerName extends Subtype[String] with VersionSpecific.ContainerNameAssertion {

  def unsafeMake(s: String): ContainerName = wrap(s)

  implicit val ContainerNameCodec: JsonCodec[ContainerName.Type] =
    JsonCodec.string.transformOrFail(s => validationToEither(ContainerName.make(s)), unwrap(_))

}
