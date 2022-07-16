package io.github.scottweaver.zillen.models

import zio.test._
import zio.json._
import zio._

object EncodingSpec extends ZIOSpecDefault {

  val createImageResponseSuite = suite("CreateImageResponse")(
    test("[SCENARIO #1] should decode from JSON to a CreateImageResponse.") {
      val json     =
        """{"status":"Downloading","progressDetail":{"current":1952210,"total":1952210},"progress":"[==================================================>]  1.952MB/1.952MB","id":"2a3ebcb7fbcc"}"""
      val response = json.fromJson[CreateImageResponse]

      ZIO.fromEither(response).map { response =>
        assertTrue(
          response.status == "Downloading",
          response.id.get == CreateImageResponse.Id("2a3ebcb7fbcc"),
          response.progressDetail.get.current == 1952210,
          response.progressDetail.get.total == 1952210,
          response.progress.get == "[==================================================>]  1.952MB/1.952MB"
        )
      }
    },
    test("[SCENARIO #2] should decode from JSON to a CreateImageResponse.") {
      val json     = """{"status":"Pulling from library/alpine","id":"latest"}"""
      val response = json.fromJson[CreateImageResponse]

      ZIO.fromEither(response).map { response =>
        assertTrue(
          response.status == "Pulling from library/alpine",
          response.id.get == CreateImageResponse.Id("latest"),
          response.progressDetail.isEmpty
        )
      }
    }
  )

  val portSuite =
    suite("Port")(
      test("""should encode an exposed port to the bespoke docker format e.g. port 80 on TCP = `{"80/tcp":{}}`.""") {

        val exposedPorts = Port.Exposed.make(Port.makeTCPPort(80))

        val json = exposedPorts.toJson

        assertTrue(
          json == """{"80/tcp":{}}"""
        )
      }
    )

  def spec =
    suite("JsonCodecSpec")(
      createImageResponseSuite,
      portSuite
    )

}
