package io.github.scottweaver.zillen.models

import zio.prelude._

object VersionSpecific {

  trait ContainerNameAssertion { self: Subtype[String] =>
    override def assertion = assert {
     Assertion.matches("""^/?[a-zA-Z0-9][a-zA-Z0-9_.-]+$""")
    }

  }

  trait HostPortValidation { self: Subtype[Int] =>
    override def assertion = assert {
      Assertion.between(1, 65535)
    }
    
  }

}