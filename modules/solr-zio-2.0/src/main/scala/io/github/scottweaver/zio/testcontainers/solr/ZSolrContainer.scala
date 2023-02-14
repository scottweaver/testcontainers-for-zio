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

object ZSolrContainer {
  type Settings = SolrContainer.Def

  object Settings {
    val default: ULayer[Settings] = ZLayer.succeed(SolrContainer.Def())
  }

  val live: ZLayer[Settings, Nothing, SolrContainer] = ZLayer.scoped {
    for {
      settings  <- ZIO.service[Settings]
      container <- makeContainer(settings)
    } yield container
  }

  def makeContainer(settings: Settings): ZIO[Scope, Nothing, SolrContainer] =
    ZIO.acquireRelease(ZIO.attempt(settings.start()).orDie)(container =>
      ZIO
        .attempt(container.stop())
        .ignoreLogged
    )
}
