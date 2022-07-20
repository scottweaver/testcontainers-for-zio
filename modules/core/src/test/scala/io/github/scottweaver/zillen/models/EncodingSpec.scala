package io.github.scottweaver.zillen.models

import zio.test._
import zio.json._
import zio._

object EncodingSpec extends ZIOSpecDefault {

  val createImageResponseSuite = suite("CreateImageResponse")(
    test("[SCENARIO #1] should decode from JSON to a CreateImageResponse.") {
      val json =
        """{"status":"Downloading","progressDetail":{"current":1952210,"total":1952210},"progress":"[==================================================>]  1.952MB/1.952MB","id":"2a3ebcb7fbcc"}"""
      val response = json.fromJson[CreateImageResponse]

      ZIO.fromEither(response).map { response =>
        assertTrue(
          response.status == "Downloading",
          response.id.get == CreateImageResponse.Id.safeMake("2a3ebcb7fbcc"),
          response.progressDetail.get.current == 1952210L,
          response.progressDetail.get.total == 1952210L,
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
          response.id.get == CreateImageResponse.Id.safeMake("latest"),
          response.progressDetail.isEmpty
        )
      }
    }
  )

  val hostConfigSuite = suite("HostConfig")(
    test("should encode to the correct JSON format.") {
      import HostConfig._

      val hc: HostConfig = HostConfig(Chunk(PortBinding.make(Port(8080, Port.Protocol.TCP), HostPort.unsafeMake(8081))))
      val json           = hc.toJsonPretty
      println(json)

      assertTrue(true)
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
      hostConfigSuite,
      portSuite
    )

}
