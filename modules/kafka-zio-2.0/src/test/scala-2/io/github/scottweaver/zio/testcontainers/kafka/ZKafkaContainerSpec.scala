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

import ch.qos.logback.classic.LoggerContext
import com.dimafeng.testcontainers.KafkaContainer
import org.slf4j.LoggerFactory
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import zio._
import zio.kafka.consumer.{Consumer, ConsumerSettings, Subscription}
import zio.kafka.producer.{Producer, ProducerSettings}
import zio.kafka.serde.Serde
import zio.test.Assertion._
import zio.test._

import TestAspect._

object ZKafkaContainerSpec extends ZIOSpecDefault {
  def spec =
    suite("ZKafkaContainerSpec")(
      test("SLF4J uses the configured Logback provider") {
        assertTrue(LoggerFactory.getILoggerFactory.isInstanceOf[LoggerContext])
      },
      test("Should start a Kafka container and provide the expected producer and consumer settings.") {
        val testCase = for {
          container        <- ZIO.service[KafkaContainer]
          consumerSettings <- ZIO.service[ConsumerSettings]
          producerSettings <- ZIO.service[ProducerSettings]
        } yield {
          val expectedBootstrapServers = Some(container.bootstrapServers)
          assert(consumerSettings.properties.get(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG))(
            equalTo(expectedBootstrapServers)
          ) &&
          assert(producerSettings.properties.get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG))(
            equalTo(expectedBootstrapServers)
          ) &&
          assert(consumerSettings.properties.get(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG))(equalTo(Some("earliest")))
        }

        testCase
      },
      test("Producing and consuming should work as expected") {

        val producer = ZLayer.fromZIO {

          ZIO.serviceWithZIO[ProducerSettings](settings => Producer.make(settings).orDie)

        }

        val consumer = ZLayer.fromZIO {
          ZIO.serviceWithZIO[ConsumerSettings](settings => Consumer.make(settings).orDie)
        }

        val testCase = for {
          producerService <- ZIO.service[Producer]
          consumerService <- ZIO.service[Consumer]
          _               <- producerService.produce("test-topic", "test-key", "test-value", Serde.string, Serde.string)
          result <- consumerService
                      .plainStream(Subscription.topics("test-topic"), Serde.string, Serde.string)
                      .take(1)
                      .runLast
                      .map {
                        case Some(record) => record.value
                        case None         => "fail"
                      }
        } yield assert(result)(equalTo("test-value"))

        testCase
          .provideSome[ProducerSettings & ConsumerSettings & Scope](producer, consumer)
      }
    )
      .provideSomeShared[Scope & Annotations](
        ZKafkaContainer.Settings.default,
        ZKafkaContainer.live,
        ZKafkaContainer.defaultConsumerSettings,
        ZKafkaContainer.defaultProducerSettings
      ) @@ withLiveClock @@ timeout(5.minutes)
}
