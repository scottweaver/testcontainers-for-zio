package io.github.scottweaver.zillen

import zio.ZLayer
import io.netty.bootstrap.Bootstrap

package object netty {
  val nettyBootstrapLayer = ZLayer.succeed(new Bootstrap)
}
