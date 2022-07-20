package io.github.scottweaver.zio.testcontainers.kafka

import zio._
import zio.duration._
import com.dimafeng.testcontainers.KafkaContainer
import zio.kafka.consumer.ConsumerSettings
import org.apache.kafka.clients.consumer.ConsumerConfig
import zio.kafka.consumer.Consumer
import zio.kafka.producer.ProducerSettings

object ZKafkaContainer {

  type Settings = KafkaContainer.Def

  object Settings {
    val default = ZLayer.succeed(KafkaContainer.Def())
  }

  val live = {

    def makeContainer(settings: Settings) =
      ZManaged.make(
        ZIO.effect {
          settings.start()
        }.orDie
      )(container =>
        ZIO
          .effect(container.stop())
          .tapError(e => ZIO.effect(println(s"Error stopping container: $e")))
          .ignore
      )

    ZLayer.fromManaged {
      for {
        settings  <- ZIO.service[Settings].toManaged_
        container <- makeContainer(settings)
      } yield container
    }
  }

  val defaultConsumerSettings =
    ZLayer.fromService { (container: KafkaContainer) =>
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
        .withPerPartitionChunkPrefetch(16)
        .withOffsetRetrieval(Consumer.OffsetRetrieval.Auto())
    }

  val defaultProducerSettings = ZLayer.fromService { (container: KafkaContainer) =>
    ProducerSettings(container.bootstrapServers.split(',').toList)
  }

}
