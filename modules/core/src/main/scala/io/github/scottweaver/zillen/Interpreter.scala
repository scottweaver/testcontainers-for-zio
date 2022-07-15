package io.github.scottweaver.zillen

import zio._
import zhttp.service.{ ChannelFactory, Client, EventLoopGroup }
import io.github.scottweaver.zillen.Command.Pull

trait Interpreter {
  def execute(command: Command): ZIO[Any, Throwable, Unit]
}

object Interpreter {

  val localSocket = ZLayer.fromZIO {
    
    (for {
      elg <- ZIO.service[EventLoopGroup]
      cf <- ZIO.service[ChannelFactory]

    } yield (InterpreterLive(cf, elg))).provide(
      ChannelFactory.epoll,
      EventLoopGroup.epoll()
    )
  }
}

final case class InterpreterLive(channelFactory: ChannelFactory, eventLoopGroup: EventLoopGroup) extends Interpreter {

  val env = ZEnvironment(channelFactory, eventLoopGroup)
  def execute(command: Command): ZIO[Any, Throwable, Unit] = {
   val result = command match {
      case Pull(image) =>
        val uri = s"http://localhost/v1.41/images/create?fromImage=${image}"

        for {
          res  <- Client.request(uri)
          data <- res.bodyAsString
          _    <- Console.printLine(data)
        } yield ()

    }

    result.provideEnvironment(env)
  }

}
