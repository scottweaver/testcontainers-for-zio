package io.github.scottweaver.zio.aspect

import zio.test._
import zio.test.TestAspect._
import io.github.scottweaver.zio.testcontainers.mysql.ZMySQLContainer

object LiquibaseAspectSpec extends ZIOSpecDefault {

  def spec =
    suite("LiquibaseAspectSpec")(
      test("The migration runs as expected.") {
        assertTrue(true)

      } @@ LiquibaseAspect.migrate("classpath://changelog.yaml")
    )
      .provideShared(
        ZMySQLContainer.Settings.default,
        ZMySQLContainer.live,
        LiquibaseAspect.Settings.default
      ) @@ sequential

}
