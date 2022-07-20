package io.github.scottweaver.zillen.models

import zio.prelude._

object VersionSpecific {

  trait ContainerNameAssertion { self: Subtype[String] =>
    override inline def assertion = 
     Assertion.matches("""^/?[a-zA-Z0-9][a-zA-Z0-9_.-]+$""")
  }


  trait HostPortValidation { self: Subtype[Int] =>
    override inline def assertion =      Assertion.between(1, 65535)
  }
}