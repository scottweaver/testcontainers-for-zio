package io.github.scottweaver.zillen.netty

import io.netty.channel._
import io.netty.handler.codec.http._
import java.nio.charset.Charset

class ResponseBodyHandler(val callback: (String) => Unit) extends SimpleChannelInboundHandler[HttpContent] {
  override def channelRead0(ctx: ChannelHandlerContext, msg: HttpContent): Unit =
    msg match {
      case content: DefaultHttpContent =>
        val body = content.content().toString(Charset.defaultCharset())
        callback(body)
      case _                           =>
        callback("")
    }
}
