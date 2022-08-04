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

package io.github.scottweaver.zillen

import zio._
import zio.test._

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
