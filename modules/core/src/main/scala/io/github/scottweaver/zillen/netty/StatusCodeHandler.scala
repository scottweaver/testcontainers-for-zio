package io.github.scottweaver
package zillen
package netty

import io.netty.channel._
import io.netty.handler.codec.http.HttpResponse

private[zillen] class StatusCodeHandler(callback: (Int) => Unit) extends ChannelInboundHandlerAdapter {
  override def channelRead(ctx: ChannelHandlerContext, msg: Any): Unit = {
    msg match {
      case response: HttpResponse =>
        callback(response.status().code())
      case _ =>
    }
    super.channelRead(ctx, msg)
  }
}
