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

package io.github.scottweaver.zio.testcontainers.solr

import com.dimafeng.testcontainers.SolrContainer
import zio._
import zio.http._
import zio.http.model._
import zio.test._

object ZSolrContainerSpec extends ZIOSpecDefault {
  override val spec =
    suite("ZSolrContainerSpec")(
      test("Should start up a Solr continer that is ready") {
        for {
          container <- ZIO.service[SolrContainer]
          client    <- ZIO.service[Client]
          resp <-
            client.host(container.host).port(container.solrPort).get(s"/solr/${container.collectionName}/admin/ping")
        } yield assertTrue(
          resp.status == Status.Ok
        )
      }.provideShared(ZSolrContainer.Settings.default >>> ZSolrContainer.live, Client.default)
    )
}
