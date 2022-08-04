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

import io.netty.channel._
import io.netty.handler.codec.http._

import java.nio.charset.Charset

private[zillen] class ResponseContentHandler(val callback: (String, Boolean) => Unit)
    extends ChannelInboundHandlerAdapter {
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
