package io.github.scottweaver
package zillen
package netty

import io.netty.channel._
import io.netty.handler.codec.http._
import java.nio.charset.Charset

private[zillen] class ResponseContentHandler(val callback: (String, Boolean) => Unit) extends ChannelInboundHandlerAdapter {
  override def channelRead(ctx: ChannelHandlerContext, msg: Object): Unit = {
    msg match {
      case content: DefaultHttpContent =>
        val body = content.content().toString(Charset.defaultCharset())
        callback(body, false)
      case content: FullHttpResponse =>
        val body = content.content().toString(Charset.defaultCharset())
        callback(body, true)
      case content: LastHttpContent =>
        val body = content.content().toString(Charset.defaultCharset())
        callback(body, true)
      case _ =>
    }

    super.channelRead(ctx, msg)
  }
}
