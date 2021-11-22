# Testcontainers for ZIO

Provides idiomatic, easy-to-use ZLayers for (Testcontainers-scala)[https://github.com/testcontainers/testcontainers-scala].

## Testcontainers Best-Practices

- Make sure your test configuration has the following settings `Test / fork := true`. Withou this  the Docker container created by the test will NOT be cleaned up until you exit SBT/the JVM process.  This could quickly run your machine out of resources as you will end up with a ton of orphaned containers running.
- Use `provideLayerShared({container}.live)` on your suite so that each test case isn't spinning up and down the container.

## MySQL

Provides a managed ZLayer that starts and stops a `com.dimafeng.testcontainers.MySQLTestContainer` as well as also provding a managed `java.sql.Connection`.

See test cases for example uages.

## Kafka

Provides a managed ZLayer that starts and stops a `com.dimafeng.testcontainers.KafkaContainer`.

You also have easy access to:
- `zio.kafka.consumer.ConsumerSettings` via `ZKafkaContainer.defaultConsumerSettings`.
- `zio.kafka.consumer.ProducerSettings` via `ZKafkaContainer.defaultProducerSettings`.

You can use these to create a `zio.kafka.consumer.Consumer` and/or `zio.kafka.producer.Producer` that can be used to interact with the Kafka container instance.

See test cases for example uages.