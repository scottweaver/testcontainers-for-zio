package io.github.scottweaver.zillen

import io.github.scottweaver.zillen.netty._

object Docker {

  def layer(builder: DockerSettings => DockerSettings = identity) =
    DockerSettings.default(builder)  >+>  (nettyBootstrapLayer >>> NettyRequestHandler.layer >>> Interpreter.layer)
}
