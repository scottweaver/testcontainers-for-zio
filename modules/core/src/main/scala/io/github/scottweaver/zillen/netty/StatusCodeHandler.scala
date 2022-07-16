package io.github.scottweaver.zillen.netty

import io.netty.channel._
import io.netty.handler.codec.http.HttpResponse

class StatusCodeHandler(callback: (Int) => Unit ) extends SimpleChannelInboundHandler[HttpResponse] {
  override def channelRead0(ctx: ChannelHandlerContext, msg: HttpResponse): Unit = {
    callback(msg.status().code())
  }
}
  
