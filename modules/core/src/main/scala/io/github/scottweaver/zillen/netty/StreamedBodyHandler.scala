package io.github.scottweaver
package zillen
package netty

import io.netty.channel._
import zio._
import zio.stream._
import io.netty.handler.codec.http._
import java.io._
import java.nio.charset.Charset
import io.netty.buffer.ByteBuf

private[zillen] class StreamedBodyHandler(callback: () => Unit) extends ChannelInboundHandlerAdapter {

  private val pos = new PipedOutputStream()
  private val pis = new PipedInputStream()
  pis.connect(pos)

  val stream = ZStream
    .fromInputStream(pis)
    .mapAccum(Chunk.empty[Byte]) { (acc, b) =>
      def isCRLF(b: Byte) = b == 10 || b == 13

      if (isCRLF(b)) {
        val noCRLF = acc.filterNot(isCRLF).toArray
        val str    = new String(noCRLF, Charset.forName("UTF-8"))
        (Chunk.empty, Some(str))
      } else
        (acc :+ b, None)
    }
    .collect { case Some(s) => s }
    .filterNot(_.isEmpty)

  private def writeToStream(byteBuf: ByteBuf) =
    if (byteBuf.isReadable()) {
      val rb   = byteBuf.readableBytes()
      val buff = new Array[Byte](rb)
      byteBuf.readBytes(buff)
      pos.write(buff)
      pos.flush()
    }

  override def channelRead(ctx: ChannelHandlerContext, msg: Any): Unit = {

    msg match {
      case content: DefaultHttpContent =>
        writeToStream(content.content())
      case content: FullHttpResponse =>
        writeToStream(content.content())
        try pos.close()
        finally (callback())
      case _: LastHttpContent =>
        try pos.close()
        finally (callback())
      case _ =>
    }

    super.channelRead(ctx, msg)
  }
}
