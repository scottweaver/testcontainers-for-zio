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

import zio._
import zio.magic._
import zio.test._
import zio.test.Assertion._
import zio.kafka.consumer.ConsumerSettings
import com.dimafeng.testcontainers.KafkaContainer
import zio.kafka.producer.ProducerSettings
import zio.kafka.producer.Producer
import zio.blocking.Blocking
import zio.kafka.consumer.Consumer
import zio.clock.Clock
import zio.kafka.serde.Serde
import zio.kafka.consumer.Subscription

object ZKafkaContainerSpec extends DefaultRunnableSpec {
  def spec =
    suite("ZKafkaContainerSpec")(
      testM("Should start a Kafka container and provide the expected producer and consumer settings.") {
        val testCase = for {
          container        <- ZIO.service[KafkaContainer]
          consumerSettings <- ZIO.service[ConsumerSettings]
          producerSettings <- ZIO.service[ProducerSettings]
        } yield {
          val expectedBootstrapServers = container.bootstrapServers.split(',').toList
          assert(consumerSettings.bootstrapServers)(equalTo(expectedBootstrapServers)) &&
          assert(producerSettings.bootstrapServers)(equalTo(expectedBootstrapServers))
        }

        testCase
      },
      testM("Producing and consuming should work as expected") {

        val producer = ZLayer.fromServiceManaged { settings: ProducerSettings => Producer.make(settings).orDie }

        val consumer = ZLayer.fromServiceManaged { settings: ConsumerSettings => Consumer.make(settings).orDie }

        val testCase = for {
          _ <- Producer.produce("test-topic", "test-key", "test-value", Serde.string, Serde.string)
          result <- Consumer
                      .subscribeAnd(Subscription.topics("test-topic"))
                      .plainStream(Serde.string, Serde.string)
                      .take(1)
                      .runLast
                      .map {
                        case Some(record) => record.value
                        case None         => "fail"
                      }
        } yield assert(result)(equalTo("test-value"))

        testCase
          .injectSome[Has[ProducerSettings] with Has[ConsumerSettings] with Blocking](producer, consumer, Clock.live)
      }
    ).provideSomeLayerShared[Blocking](
      ZKafkaContainer.Settings.default >>> ZKafkaContainer.live >+> ZKafkaContainer.defaultConsumerSettings >+> ZKafkaContainer.defaultProducerSettings
    )
}
