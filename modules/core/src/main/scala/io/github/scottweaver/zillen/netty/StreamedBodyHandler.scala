/*
 * Copyright 2021 io.github.scottweaver
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.scottweaver
package zillen
package netty

import io.netty.buffer.ByteBuf
import io.netty.channel._
import io.netty.handler.codec.http._
import zio._
import zio.stream._

import java.io._
import java.nio.charset.Charset

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
      } else (acc :+ b, None)
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
