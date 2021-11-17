# Testcontainers for ZIO

Provides idiomatic, easy-to-use ZLayers for (Testcontainers-scala)[https://github.com/testcontainers/testcontainers-scala].

## Testcontainers Best-Practices

- Make sure your test configuration has the following settings `Test / fork := true`. Withou this  the Docker container created by the test will NOT be cleaned up until you exit SBT/the JVM process.  This could quickly run your machine out of resources as you will end up with a ton of orphaned containers running.
- Use `provideLayerShared({container}.live)` on your suite so that each test case isn't spinning up and down the container.

## MySQL

Provides a managed ZLayer that starts and stops a `MySQLTestContainer` as well as also provding a managed `java.sql.Connection`.