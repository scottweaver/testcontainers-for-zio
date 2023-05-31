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

package io.github.scottweaver.zio.testcontainers.kafka

import com.dimafeng.testcontainers.KafkaContainer
import org.apache.kafka.clients.consumer.ConsumerConfig
import zio._
import zio.kafka.consumer.{Consumer, ConsumerSettings}
import zio.kafka.producer.ProducerSettings

object ZKafkaContainer {

  type Settings = KafkaContainer.Def

  object Settings {
    val default = ZLayer.succeed(KafkaContainer.Def())
  }

  val live = ZLayer.scoped {
    for {
      settings  <- ZIO.service[Settings]
      container <- makeContainer(settings)
    } yield container
  }

  def makeContainer(settings: Settings) =
    ZIO.acquireRelease(ZIO.attempt(settings.start()).orDie)(container =>
      ZIO
        .attempt(container.stop())
        .ignoreLogged
    )

  val defaultConsumerSettings =
    ZLayer.fromZIO {
      ZIO.serviceWith { (container: KafkaContainer) =>
        ConsumerSettings(container.bootstrapServers.split(',').toList)
          .withClientId("test-client-id")
          .withGroupId("test-group-id")
          .withCloseTimeout(5.seconds)
          .withProperties(
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG -> "earliest",
            ConsumerConfig.METADATA_MAX_AGE_CONFIG  -> "100",
            // ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG       -> "3000",
            ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG    -> "250",
            ConsumerConfig.MAX_POLL_RECORDS_CONFIG         -> "10",
            ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG -> "true"
          )
          .withOffsetRetrieval(Consumer.OffsetRetrieval.Auto())
      }
    }

  val defaultProducerSettings = ZLayer.fromZIO {
    ZIO.serviceWith[KafkaContainer](container => ProducerSettings(container.bootstrapServers.split(',').toList))

  }

}
