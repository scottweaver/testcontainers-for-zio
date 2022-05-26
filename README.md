# Testcontainers for ZIO


<!-- | ![Build Status][Badge-CircelCI] | -->


<!-- [Badge-CircelCI]: https://circleci.com/gh/scottweaver/testcontainers-for-zio.svg?style=shield "CircleCI Badge" -->

[Link-Github]: https://github.com/scottweaver/testcontainers-for-zio "Github Repo Link"

[Link-SonatypeReleases-Kafka]: https://oss.sonatype.org/content/repositories/releases/io/github/scottweaver/zio-testcontainers-kafka_2.13/0.5.0/  "Sonatype Releases link"
[Badge-SonatypeReleases-Kafka]: https://img.shields.io/maven-central/v/io.github.scottweaver/zio-testcontainers-kafka_2.13/0.5.0?label=maven-central%20%20kafka "Sonatype Releases badge"


[Link-SonatypeReleases-cassandra]: https://oss.sonatype.org/content/repositories/releases/io/github/scottweaver/zio-testcontainers-cassandra_2.13/0.5.0/  "Sonatype Releases link"
[Badge-SonatypeReleases-cassandra]: https://img.shields.io/maven-central/v/io.github.scottweaver/zio-testcontainers-cassandra_2.13/0.5.0?label=maven-central%20%20cassandra "Sonatype Releases badge"


[Link-SonatypeReleases-MySQL]: https://oss.sonatype.org/content/repositories/releases/io/github/scottweaver/zio-testcontainers-mysql_2.13/0.5.0/  "Sonatype Releases link"
[Badge-SonatypeReleases-MySQL]: https://img.shields.io/maven-central/v/io.github.scottweaver/zio-testcontainers-mysql_2.13/0.5.0?label=maven-central%20%20mysql "Sonatype Releases badge"

[Link-SonatypeReleases-Postgresql]: https://oss.sonatype.org/content/repositories/releases/io/github/scottweaver/zio-testcontainers-postgresql_2.13/0.5.0/  "Sonatype Releases link"
[Badge-SonatypeReleases-Postgresql]: https://img.shields.io/maven-central/v/io.github.scottweaver/zio-testcontainers-postgresql_2.13/0.5.0?label=maven-central%20%20postgresql "Sonatype Releases badge"

[Link-SonatypeReleases-DbMigrationAspect]: https://oss.sonatype.org/content/repositories/releases/io/github/scottweaver/zio-db-migration-aspect_2.13/0.5.0/  "Sonatype Releases link"
[Badge-SonatypeReleases-DbMigrationAspect]: https://img.shields.io/maven-central/v/io.github.scottweaver/zio-db-migration-aspect_2.13/0.5.0?label=maven-central%20%20db-migration-aspect "Sonatype Releases badge"

Provides idiomatic, easy-to-use ZLayers for [Testcontainers-scala](https://github.com/testcontainers/testcontainers-scala).


| CI                              | 
| ------------------------------- |
| [![CircleCI](https://dl.circleci.com/insights-snapshot/gh/scottweaver/testcontainers-for-zio/main/test/badge.svg?window=30d)](https://app.circleci.com/insights/github/scottweaver/testcontainers-for-zio/workflows/test/overview?branch=main&reporting-window=last-30-days&insights-snapshot=true) | 

## Testcontainers Best-Practices

- Make sure your test configuration has the following settings `Test / fork := true`. Without this  the Docker container created by the test will NOT be cleaned up until you exit SBT/the JVM process.  This could quickly run your machine out of resources as you will end up with a ton of orphaned containers running.
- Use `provideLayerShared({container}.live)` on your suite so that each test case isn't spinning up and down the container.

## Cassandra

[![Release Artifacts][Badge-SonatypeReleases-cassandra]][Link-SonatypeReleases-cassandra]

Provides a managed ZLayer that starts and stops a `com.dimafeng.testcontainers.CassandraContainer` as well as also provding a managed `com.datastax.oss.driver.api.core.CqlSession`.

### ZIO 1.x
```scala
libraryDependencies += "io.github.scottweaver" %% "zio-testcontainers-cassandra" % "0.5.0"
```

### ZIO 2.x
```scala
libraryDependencies += "io.github.scottweaver" %% "zio-2-0-testcontainers-cassandra" % "0.5.0"
```

See test cases for example usage.

## MySQL

[![Release Artifacts][Badge-SonatypeReleases-MySQL]][Link-SonatypeReleases-MySQL]

Provides a managed ZLayer that starts and stops a `com.dimafeng.testcontainers.MySQLTestContainer` as well as also provding a managed `java.sql.Connection`.

### ZIO 1.x
```scala
libraryDependencies += "io.github.scottweaver" %% "zio-testcontainers-mysql" % "0.5.0"
```

### ZIO 2.x
```scala
libraryDependencies += "io.github.scottweaver" %% "zio-2-0-testcontainers-mysql" % "0.5.0"
```

See test cases for example usage.

## PostgreSQL

[![Release Artifacts][Badge-SonatypeReleases-Postgresql]][Link-SonatypeReleases-Postgresql]

Provides a managed ZLayer that starts and stops a `com.dimafeng.testcontainers.PostgreSQLContainer` as well as also provding a managed `java.sql.Connection`.

### ZIO 1.x
```scala
libraryDependencies += "io.github.scottweaver" %% "zio-testcontainers-postgresql" % "0.5.0"
```

### ZIO 2.x
```scala
libraryDependencies += "io.github.scottweaver" %% "zio-2-0-testcontainers-postgresql" % "0.5.0"
```

See test cases for example usage.

## Kafka

[![Release Artifacts][Badge-SonatypeReleases-Kafka]][Link-SonatypeReleases-Kafka]

Provides a managed ZLayer that starts and stops a `com.dimafeng.testcontainers.KafkaContainer`.

You also have easy access to:
- `zio.kafka.consumer.ConsumerSettings` via `ZKafkaContainer.defaultConsumerSettings`.
- `zio.kafka.consumer.ProducerSettings` via `ZKafkaContainer.defaultProducerSettings`.

You can use these to create a `zio.kafka.consumer.Consumer` and/or `zio.kafka.producer.Producer` that can be used to interact with the Kafka container instance.


### ZIO 1.x
```scala
libraryDependencies += "io.github.scottweaver" %% "zio-testcontainers-kafka" % "0.5.0"
```

See test cases for example usage.

## Database Migrations Aspect

[![Release Artifacts][Badge-SonatypeReleases-DbMigrationAspect]][Link-SonatypeReleases-DbMigrationAspect]

Not really a test container, useful none the less.  

The `io.github.scottweaver.zio.aspect.DatabaseMigrationsAspect` provides a [ZIO TestAspect](https://javadoc.io/doc/dev.zio/zio-test_2.13/1.0.12/zio/test/TestAspect.html) for running database migrations via [Flyway](https://flywaydb.org/).  It seemlessly integrates with the `ZMySQLContainer` by using the `io.github.scottweaver.zio.models.JdbcInfo` provided by `ZMySQLContainer.live` to run your migrations.

If you are not using `ZMySQLContainer` you can just manually provide an appropriate `JdbcInfo` as a `ZLayer` to your tests that are using the `DbMigrationAspect`.

### ZIO 1.X
```scala
libraryDependencies += "io.github.scottweaver" %% "zio-db-migration-aspect" % "0.5.0"
```

### ZIO 2.X
```scala
libraryDependencies += "io.github.scottweaver" %% "zio-2-0-db-migration-aspect" % "0.5.0"
```

## Cassandra Migrations Aspect

[![Release Artifacts][Badge-SonatypeReleases-CassandraMigrationAspect]][Link-SonatypeReleases-CassandraMigrationAspect]

Not really a test container, useful none the less.

The `io.github.scottweaver.zio.aspect.CassandraMigrationsAspect` provides a [ZIO TestAspect](https://javadoc.io/doc/dev.zio/zio-test_2.13/1.0.12/zio/test/TestAspect.html) for running database migrations via [Cassandra Schema Migration](https://github.com/patka/cassandra-migration).  It seemlessly integrates with the `ZCassandraContainer` by using the `com.datastax.oss.driver.api.core.CqlSession` provided by `ZCassandraContainer.live` to run your migrations.

If you are not using `ZCassandraContainer` you can just manually provide an appropriate `com.datastax.oss.driver.api.core.CqlSession` as a `ZLayer` to your tests that are using the `CassandraMigrationAspect`.

### ZIO 1.X
```scala
libraryDependencies += "io.github.scottweaver" %% "zio-cassandra-migration-aspect" % "0.5.0"
```

### ZIO 2.X
```scala
libraryDependencies += "io.github.scottweaver" %% "zio-2-0-cassandra-migration-aspect" % "0.5.0"
```


## References

- [Working with shared dependencies in ZIO Test](https://hmemcpy.com/2021/11/working-with-shared-dependencies-in-zio-test/)
- [Speeding Up Integration Testing Through Dependency Sharing by Balazs Zagyvai](https://www.youtube.com/watch?v=PJTn33Qj1nc)