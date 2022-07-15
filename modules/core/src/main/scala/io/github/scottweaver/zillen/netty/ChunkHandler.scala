package io.github.scottweaver.zillen.netty

import io.netty.channel._

class ChunkHandler extends ChannelInboundHandlerAdapter {

  override def channelRead(ctx: ChannelHandlerContext, msg: Any): Unit = {
    println(s"[CHONKERS!] >>> chunk received ${msg}")
    super.channelRead(ctx, msg)
  }
}
  

