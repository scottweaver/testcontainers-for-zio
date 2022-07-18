package io.github.scottweaver.zillen.netty

import zio._

object Network {

  def findFreePort: RIO[Scope, Int] =
    ZIO
      .acquireRelease(
        ZIO.attemptBlocking(new java.net.ServerSocket(0))
      )(socket => ZIO.attemptBlocking(socket.close()).ignoreLogged)
      .map(_.getLocalPort)

  {}
}
