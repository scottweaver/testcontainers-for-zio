package io.github.scottweaver.zillen.models

import zio.test._
import zio.json._

object EncodingSpec extends ZIOSpecDefault {

  def spec =
    suite("EncodingSpec")(
      test("verify exposed port json conforms to what Docker API expects") {

        val exposedPorts = Port.Exposed.make(Port.makeTCPPort(80))

        val json = exposedPorts.toJson
        println(json)

        assertTrue(
          json == """{"80/tcp":{}}"""
        )
      }
    ) 
  
}
