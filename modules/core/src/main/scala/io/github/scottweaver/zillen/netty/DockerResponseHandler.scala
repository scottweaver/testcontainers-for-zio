package io.github.scottweaver.zillen.netty

import io.netty.channel._
import zio._
import zio.stream._
import io.netty.handler.codec.http._
import java.io._
import java.nio.charset.Charset
import java.util.concurrent.atomic.AtomicInteger
import io.netty.buffer.ByteBuf

class DockerResponseHandler extends ChannelInboundHandlerAdapter {

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

  val httpStatus: AtomicInteger = new AtomicInteger(Integer.MIN_VALUE)
  private var statusCode: Int   = Integer.MIN_VALUE
  private var byteBuf           = Option.empty[ByteBuf]

  override def channelRead(ctx: ChannelHandlerContext, msg: Any): Unit = {

    def writeToStream() = byteBuf match {
      case Some(byteBuf) =>
        val rb = byteBuf.readableBytes()

        if (byteBuf.isReadable()) {
          val buff = new Array[Byte](rb)
          byteBuf.readBytes(buff)
          pos.write(buff)
          pos.flush()
        }
      case None          =>
    }

    def setByteBuf(byteBuf0: ByteBuf): Unit =
      if (byteBuf.isEmpty)
        byteBuf = Some(byteBuf0.retain())
      else
        ()

    msg match {
      case response: HttpResponse =>
        this.statusCode = response.status().code()
      // IMPORTANT: LastHttpContent must appear before HttpContent as it is a subclass of HttpContent.
      case end: LastHttpContent   =>
        setByteBuf(end.content())
        writeToStream()
        httpStatus.set(statusCode)
        try {
          pos.close()
          pis.close()
        } finally ()
      case content: HttpContent   =>
        setByteBuf(content.content())
        writeToStream()
      case _                      =>
    }

    super.channelRead(ctx, msg)
  }
}

object ChunkHandler {}
