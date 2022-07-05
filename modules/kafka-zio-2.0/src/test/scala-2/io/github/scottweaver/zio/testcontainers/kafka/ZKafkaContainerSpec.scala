package io.github.scottweaver.zio.testcontainers.kafka

import zio._
import zio.test._
import zio.test.Assertion._
import zio.kafka.consumer.ConsumerSettings
import com.dimafeng.testcontainers.KafkaContainer
import zio.kafka.producer.ProducerSettings
import zio.kafka.producer.Producer
import zio.kafka.consumer.Consumer
import zio.kafka.serde.Serde
import zio.kafka.consumer.Subscription
import TestAspect._

object ZKafkaContainerSpec extends ZIOSpecDefault {
  def spec =
    suite("ZKafkaContainerSpec")(
      test("Should start a Kafka container and provide the expected producer and consumer settings.") {
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
      test("Producing and consuming should work as expected") {

        val producer = ZLayer.fromZIO {

          ZIO.serviceWithZIO[ProducerSettings](settings => Producer.make(settings).orDie)

        }

        val consumer = ZLayer.fromZIO {
          ZIO.serviceWithZIO[ConsumerSettings](settings => Consumer.make(settings).orDie)
        }

        val testCase = for {
          _      <- Producer.produce("test-topic", "test-key", "test-value", Serde.string, Serde.string)
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
          .provideSome[ProducerSettings & ConsumerSettings & Scope](producer, consumer)
      }
    )
      .provideSomeShared[Scope & Annotations](
        ZKafkaContainer.Settings.default,
        ZKafkaContainer.live,
        ZKafkaContainer.defaultConsumerSettings,
        ZKafkaContainer.defaultProducerSettings
      ) @@ withLiveClock
}
