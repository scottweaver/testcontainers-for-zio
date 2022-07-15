package io.github.scottweaver.zillen.netty

import java.util.concurrent.atomic.AtomicReference
import zio.stream.ZStream
import zio._
import io.netty.channel._
import io.netty.handler.codec.http.FullHttpResponse
import java.nio.charset.Charset
import StreamingStatusLogger.StatusResponse

final class StreamingStatusLogger(val atomic: AtomicReference[Option[StatusResponse]])
    extends SimpleChannelInboundHandler[FullHttpResponse] {
  override def channelRead0(ctx: ChannelHandlerContext, msg: FullHttpResponse): Unit = {
    val content = msg.content().retain()

    import io.netty.buffer.ByteBufInputStream
    val stream = ZStream
      .fromInputStream(new ByteBufInputStream(content))
      .mapAccum(Chunk.empty[Byte]) { (acc, b) =>
        if (b == 10)
          (Chunk.empty, Some(new String(acc.toArray, Charset.forName("UTF-8"))))
        else
          (acc :+ b, None)
      }
      .collect { case Some(s) => s }

    atomic.set(Some(StatusResponse(msg.status.code, stream)))
  }
}

object StreamingStatusLogger {
  final case class StatusResponse(statusCode: Int, responseStream: ZStream[Any, Throwable, String])
}
