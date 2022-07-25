package io.github.scottweaver.zillen

import zio.test._
import zio._

object NetworkSpec extends ZIOSpecDefault {
  val spec = suite("NetworkSpec")(
    test("Should identify an open port, but not stay bound to it.") {

      val testCase = for {
        openPort <- Network.findOpenPort
        port <-
          ZIO.acquireRelease(ZIO.attempt(new java.net.ServerSocket(openPort)))(p => ZIO.attempt(p.close()).ignoreLogged)
      } yield assertTrue(port.getLocalPort == openPort)

      testCase
    }
  ).provide(Network.layer, Scope.default)
}
