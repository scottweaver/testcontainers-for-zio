package io.github.scottweaver.zillen

import zio.test._
import zio._

object ReproducerSpec extends ZIOSpecDefault {

  final case class Phantom[T](value: String)
  final case class Corporeal[T](value: T)

  val spec = suite("ReproducerSpec")(
    test("Phantom type won't compile without @nowarn") {

      @scala.annotation.nowarn // <-- Comment-out to see unused compilation warning/error
      def needsPhantom[T: Tag] = ZLayer.fromZIO {
        ZIO.serviceWith[Phantom[T]](p => p.value)
      }

      val pgm = for {
        s <- ZIO.service[String]
      } yield (assertTrue(s == "SCARY!!!"))

      pgm
        .provide(
          ZLayer.succeed(Phantom[String]("SCARY!!!")),
          needsPhantom[String]
        )
    },
    test("Corporeal type will compile without any help.") {

      def needsCorporeal[T: Tag] = ZLayer.fromZIO {
        ZIO.serviceWith[Corporeal[T]](p => p.value)
      }

      val pgm = for {
        s <- ZIO.service[String]
      } yield (assertTrue(s == "Meh."))

      pgm
        .provide(ZLayer.succeed(Corporeal[String]("Meh.")), needsCorporeal[String])
    }
  )
}
