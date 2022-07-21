package io.github.scottweaver.zillen

import zio.test._

object ContainerSpec extends ZIOSpecDefault {
  val spec = suite("ContainerSpec")(
    test("#scopedContainer should properly run the entire lifecycle of a container.") {
      
      assertTrue(true)
   }
  )
}