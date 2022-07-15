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
    // .tap(b => ZIO.debug(s"byte: ${b}"))
    .mapAccum(Chunk.empty[Byte]) { (acc, b) =>
      def isCRLF(b: Byte) = b == 10 || b == 13

      if (isCRLF(b)) {
        val noCRLF = acc.filterNot(isCRLF).toArray
        val str    = new String(noCRLF, Charset.forName("UTF-8"))
        // println(s"bytes to string: '$str'")
        (Chunk.empty, Some(str))
      } else
        (acc :+ b, None)
    }
    .collect { case Some(s) => s }
  .filterNot(_.isEmpty)

  val httpStatus: AtomicInteger = new AtomicInteger(Integer.MIN_VALUE)
  private var statusCode: Int   = Integer.MIN_VALUE

  override def channelRead(ctx: ChannelHandlerContext, msg: Any): Unit = {

    def writeToStream(byteBuf: ByteBuf) =
      if (byteBuf.isReadable()) {
        val rb   = byteBuf.readableBytes()
        val buff = new Array[Byte](rb)
        byteBuf.readBytes(buff)
        pos.write(buff)
        pos.flush()
      }

    msg match {
      case response: HttpResponse =>
        this.statusCode = response.status().code()
      // IMPORTANT: LastHttpContent must appear before HttpContent as it is a subclass of HttpContent.
      case end: LastHttpContent   =>
        writeToStream(end.content())

        try {
          pos.close()
          pis.close()
        } finally (httpStatus.set(statusCode))
      // httpStatus.set(statusCode)
      case content: HttpContent   =>
        writeToStream(content.content())
      case _                      =>
    }

    super.channelRead(ctx, msg)
  }
}

object ChunkHandler {}
