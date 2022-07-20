package io.github.scottweaver.zillen.models

import zio.prelude._
import zio.prelude.Assertion._
import zio.json._

object ContainerName extends Subtype[String] {

  override def assertion                                         = assert {
    matches("""^/?[a-zA-Z0-9][a-zA-Z0-9_.-]+$""")
  }

 def unsafeMake(s: String): ContainerName = wrap(s) 

  implicit val ContainerNameCodec: JsonCodec[ContainerName.Type] =
    JsonCodec.string.transformOrFail(s => validationToEither(ContainerName.make(s)), unwrap(_) )

}
