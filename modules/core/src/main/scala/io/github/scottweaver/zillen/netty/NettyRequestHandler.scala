package io.github.scottweaver
package zillen
package netty

import io.netty.channel.epoll._
import io.netty.channel.kqueue._
import io.netty.bootstrap.Bootstrap
import zio._
import io.netty.util.concurrent.DefaultThreadFactory
import io.netty.channel._
import io.netty.handler.codec.http._
import io.netty.handler.logging.LoggingHandler
import io.netty.channel.unix.DomainSocketAddress
import io.netty.channel.socket.DuplexChannel
import zio.stream.ZSink
import zillen._

/**
 * Inspirations and Due Respects:
 *   - https://github.com/slandelle/netty-progress/blob/master/src/test/java/slandelle/ProgressTest.java
 *   - https://github.com/dream11/zio-http
 *   - https://github.com/docker-java/docker-java
 */
private[zillen] trait NettyRequestHandler {
  def executeRequest(request: HttpRequest): Task[Int]

  def executeRequestWithResponse(request: HttpRequest): Task[(Int, String)]
}

object NettyRequestHandler {

  type ZioChannelFactory = () => ZIO[Any, Throwable, Channel]

  private[zillen] def executeRequest(request: HttpRequest) = ZIO.serviceWithZIO[NettyRequestHandler](_.executeRequest(request))

  private[zillen] def executeRequestWithResponse(request: HttpRequest) =
    ZIO.serviceWithZIO[NettyRequestHandler](_.executeRequestWithResponse(request))

  def layer = ZLayer.fromZIO {
    for {
      socketPath <- DockerSettings.socketPath
      _          <- makeEventLoopGroup
      channel    <- makeChannelFactory(socketPath)
      scope      <- ZIO.service[Scope]

    } yield NettyRequestLive(channel, scope)
  }

  private def channelInitializer[A <: Channel]() =
    new ChannelInitializer[A] {
      override def initChannel(ch: A): Unit = {
        ch.pipeline()
          .addLast(
            new LoggingHandler(getClass()),
            new HttpClientCodec(),
            new HttpContentDecompressor()
          )

        ()
      }
    }

  private def makeKQueue(bootstrap: Bootstrap) = ZIO.acquireRelease(
    ZIO.attempt {
      val evlg = new KQueueEventLoopGroup(0, new DefaultThreadFactory("zio-zillen-kqueue"))

      bootstrap
        .group(evlg)
        .channel(classOf[KQueueDomainSocketChannel])
        .handler(channelInitializer[KQueueDomainSocketChannel]())
      evlg
    }
  )(evlg => ZIO.attemptBlocking(evlg.shutdownGracefully().get()).ignoreLogged)

  private def makeEpoll(bootstrap: Bootstrap) = ZIO.acquireRelease(
    ZIO.attempt {
      val evlg = new EpollEventLoopGroup(0, new DefaultThreadFactory("zio-zillen-epoll"))

      bootstrap
        .group(evlg)
        .channel(classOf[EpollDomainSocketChannel])
        .handler(channelInitializer[EpollDomainSocketChannel]())
      evlg
    }
  )(evlg => ZIO.attemptBlocking(evlg.shutdownGracefully().get()).ignoreLogged)

  private def makeChannelFactory(path: DockerSocketPath): URIO[Bootstrap, () => Task[DuplexChannel]] =
    ZIO.serviceWith[Bootstrap] { bootstrap => () =>
      {
        val channel = ZIO.attempt {
          bootstrap.connect(new DomainSocketAddress(path)).sync().channel()
        }

        channel.flatMap {
          case c: DuplexChannel => ZIO.succeed(c)
          case other            => ZIO.fail(new Exception(s"Expected a duplex channel, got ${other.getClass.getName} instead."))
        }
      }
    }

  private def makeEventLoopGroup: ZIO[Scope with Bootstrap, Throwable, EventLoopGroup] =
    ZIO.serviceWithZIO[Bootstrap] { bootstrap =>
      if (Epoll.isAvailable())
        makeEpoll(bootstrap)
      else if (KQueue.isAvailable())
        makeKQueue(bootstrap)
      else
        ZIO.fail(new Exception("Could not create the appropriate event loop group.  Reason: OS not supported."))
    }

}

final case class NettyRequestLive(channelFactory: NettyRequestHandler.ZioChannelFactory, scope: Scope) extends NettyRequestHandler {

  override def executeRequest(request: HttpRequest): Task[Int] = {

    var statusCode          = Int.MinValue
    var bodyComplete        = false
    val streamedBodyHandler = new StreamedBodyHandler(() => bodyComplete = true)
    val statusCodeHandler   = new StatusCodeHandler((code) => statusCode = code)

    val schedule: Schedule[Any, Any, Any] =
      Schedule.recurWhile(_ => statusCode == Integer.MIN_VALUE || !bodyComplete)

    val initializedChannel = for {
      channel <- channelFactory()
      _ = channel
            .pipeline()
            .addLast("stream-body-handler", streamedBodyHandler)
            .addLast("status-code-handler", statusCodeHandler)
    } yield channel

    def execute(channel: Channel) =
      ZIO.logDebug(s"Executing request ${request}") *> ZChannelFuture.make {
        channel.writeAndFlush(request)
      }
        .flatMap(_.scoped)
        .flatMap(_ => ZIO.unit.schedule(schedule)) <* ZIO.logDebug(s"Request, ${request}, completed successfully.")

    val runningStream = streamedBodyHandler.stream.run(ZSink.foreach(ZIO.debug(_)))

    (for {
      channel <- initializedChannel
      f1      <- execute(channel).fork
      f2      <- runningStream.fork
      _       <- f1.join
      _       <- f2.join
    } yield statusCode).provide(ZLayer.succeed(scope))
  }

  def executeRequestWithResponse(request: HttpRequest): Task[(Int, String)] = {

    var responseBody     = ""
    var responseComplete = false
    var statusCode       = Int.MinValue

    val bodyHandler = new ResponseContentHandler({ case (body, done) =>
      responseBody += body
      responseComplete = done
    })

    val statusCodeHandler = new StatusCodeHandler((code) => statusCode = code)

    val schedule: Schedule[Any, Any, Any] =
      Schedule.recurWhile(_ => statusCode == Integer.MIN_VALUE || !responseComplete)

    val initializedChannel = for {
      channel <- channelFactory()
      _ = channel
            .pipeline()
            .addLast("aggregator", new HttpObjectAggregator(Int.MaxValue))
            .addLast("status-code-handler", statusCodeHandler)
            .addLast("body-content-handler", bodyHandler)
    } yield channel

    def execute(channel: Channel) =
      ZIO.logDebug(s"Executing request ${request}") *> ZChannelFuture.make {
        channel.writeAndFlush(request)
      }
        .flatMap(_.scoped)
        .flatMap(_ => ZIO.unit.schedule(schedule))
        .as(statusCode -> responseBody)

    (for {
      channel  <- initializedChannel
      response <- execute(channel)
    } yield response).provide(ZLayer.succeed(scope)) <* ZIO.logDebug(s"Request, ${request}, completed successfully.")

  }
}
