/*
 * Copyright 2021 io.github.scottweaver
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.scottweaver.zillen.models

import zio._
import zio.json._
import zio.test._

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

      val hc: HostConfig =
        HostConfig(PortMap.makeOneToOne(ProtocolPort(8080, Protocol.TCP) -> HostInterface.makeUnsafeFromPort(8081)))
      val json = hc.toJsonPretty
      println(json)

      assertTrue(true)
    }
  )

  val hostPortSuite = suite("HostPort")(
    test("should encode to the correct JSON format.") {
      val hp   = HostInterface.makeUnsafeFromPort(8081)
      val json = hp.toJsonPretty
      println(json)

      assertTrue(true)
    }
  )

  val portSuite =
    suite("ProtocolPort")(
      test("""should encode an exposed port to the bespoke docker format e.g. port 80 on TCP = `{"80/tcp":{}}`.""") {

        val exposedPorts = ProtocolPort.Exposed.make(ProtocolPort.makeTCPPort(80))

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
