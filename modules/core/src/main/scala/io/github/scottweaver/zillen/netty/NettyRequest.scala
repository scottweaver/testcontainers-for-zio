package io.github.scottweaver.zillen.netty

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

/** Inspirations and Due Respects:
  *   - https://github.com/slandelle/netty-progress/blob/master/src/test/java/slandelle/ProgressTest.java
  *   - https://github.com/dream11/zio-http
  *   - https://github.com/docker-java/docker-java
  */
trait NettyRequest {
  def executeRequest(request: HttpRequest): Task[Int]

  def executeRequestWithResponse(request: HttpRequest): Task[(Int, String)]
}

object NettyRequest {

  type ZioChannelFactory = () => ZIO[Any, Throwable, Channel]

  def executeRequest(request: HttpRequest) = ZIO.serviceWithZIO[NettyRequest](_.executeRequest(request))

  def executeRequestWithResponse(request: HttpRequest) =
    ZIO.serviceWithZIO[NettyRequest](_.executeRequestWithResponse(request))

  def live                                                                                                          = ZLayer.fromZIO {
    for {
      _       <- makeEventLoopGroup
      channel <- makeChannelFactory()
      scope   <- ZIO.service[Scope]

    } yield NettyRequestLive(channel, scope)
  }

  private def channelInitializer[A <: Channel]()                                                                    =
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

  private def makeKqueue(bootstrap: Bootstrap)                                                                      = ZIO.acquireRelease(
    ZIO.attempt {
      val evlg = new KQueueEventLoopGroup(0, new DefaultThreadFactory("zio-zillen-kqueue"))

      bootstrap
        .group(evlg)
        .channel(classOf[KQueueDomainSocketChannel])
        .handler(channelInitializer[KQueueDomainSocketChannel]())
      evlg
    }
  )(evlg => ZIO.attemptBlocking(evlg.shutdownGracefully().get()).ignoreLogged)

  private def makeEpoll(bootstrap: Bootstrap)                                                                       = ZIO.acquireRelease(
    ZIO.attempt {
      val evlg = new EpollEventLoopGroup(0, new DefaultThreadFactory("zio-zillen-epoll"))

      val channelFactory: ChannelFactory[EpollDomainSocketChannel] = () => new EpollDomainSocketChannel()

      bootstrap
        .group(evlg)
        .channelFactory(channelFactory)
        .handler(channelInitializer[EpollDomainSocketChannel]())
      evlg
    }
  )(evlg => ZIO.attemptBlocking(evlg.shutdownGracefully().get()).ignoreLogged)

  private def makeChannelFactory(path: String = "/var/run/docker.sock"): URIO[Bootstrap, () => Task[DuplexChannel]] =
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

  private def makeEventLoopGroup: ZIO[Scope with Bootstrap, Throwable, EventLoopGroup]                              =
    ZIO.serviceWithZIO[Bootstrap] { bootstrap =>
      if (Epoll.isAvailable())
        makeEpoll(bootstrap)
      else if (KQueue.isAvailable())
        makeKqueue(bootstrap)
      else
        ZIO.fail(new Exception("Could not create the appropriate event loop group.  Reason: OS not supported."))
    }

}

final case class NettyRequestLive(channelFactory: NettyRequest.ZioChannelFactory, scope: Scope) extends NettyRequest {

  override def executeRequest(request: HttpRequest): Task[Int] = {

    var statusCode        = Int.MinValue
    var bodyComplete      = false
    val streamedBodyHandler      = new StreamedBodyHandler(() => bodyComplete = true)
    val statusCodeHandler = new StatusCodeHandler((code) => statusCode = code)

    val schedule: Schedule[Any, Any, Any] =
      Schedule.recurWhile(_ => statusCode == Integer.MIN_VALUE || !bodyComplete)

    val initializedChannel = for {
      channel <- channelFactory()
      _        = channel
                   .pipeline()
                   .addLast(streamedBodyHandler)
                   .addLast(statusCodeHandler)
    } yield channel

    def execute(channel: Channel) =
      ZChannelFuture.make {

        channel.writeAndFlush(request)
      }
        .flatMap(_.scoped)
        .flatMap(_ => ZIO.unit.schedule(schedule))

    val runningStream = streamedBodyHandler.stream.run(ZSink.foreach(ZIO.debug(_)))

    (for {
      channel <- initializedChannel
      f1      <- execute(channel).fork
      f2      <- runningStream.fork
      _       <- f1.join
      _       <- ZIO.debug(s">>> STATUS CODE JOINED: ${statusCode}")
      _       <- f2.join
      _       <- ZIO.debug(">>> STREAM JOINED")
    } yield statusCode).provide(ZLayer.succeed(scope))
  }

  def executeRequestWithResponse(request: HttpRequest): Task[(Int, String)] = {

    var responseBody = Option.empty[String]
    var statusCode   = Int.MinValue

    val bodyHandler       = new ResponseBodyHandler((body) => responseBody = Some(body))
    val statusCodeHandler = new StatusCodeHandler((code) => statusCode = code)

    val schedule: Schedule[Any, Any, Any] =
      Schedule.recurWhile(_ => statusCode == Integer.MIN_VALUE || responseBody.isEmpty)

    val initializedChannel = for {
      channel <- channelFactory()
      _        = channel
                   .pipeline()
                   .addLast(statusCodeHandler)
                   .addLast(bodyHandler)
    } yield channel

    def execute(channel: Channel) =
      ZChannelFuture.make {

        channel.writeAndFlush(request)
      }
        .flatMap(_.scoped)
        .flatMap(_ => ZIO.unit.schedule(schedule))
        .as(statusCode -> responseBody.getOrElse(""))

    (for {
      channel  <- initializedChannel
      response <- execute(channel)
    } yield response).provide(ZLayer.succeed(scope))

  }
}
