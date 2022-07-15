package io.github.scottweaver.zillen.netty

// import zhttp.service.ChannelFactory
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
import java.util.concurrent.atomic.AtomicReference
import zio.stream._
import StreamingStatusLogger.StatusResponse

/**
  * 
  * Inspirations and Due Respects:
  * - https://github.com/slandelle/netty-progress/blob/master/src/test/java/slandelle/ProgressTest.java
  * - https://github.com/dream11/zio-http
  * - https://github.com/docker-java/docker-java
  */
object NettyRequest {

  def executeRequest(request: HttpRequest) = {
    val atomicStream = new AtomicReference[Option[StatusResponse]](Option.empty)
    // val atomicStatus = new AtomicReference[Option[ResponseStatusHandler.ResponseStatus]](Option.empty)
    ZIO.serviceWithZIO[Channel] { channel =>
      zhttp.service.ChannelFuture.make {

        channel
          .pipeline()
          .addLast(new ChunkHandler)
          // .addLast(new ResponseStatusHandler(atomicStatus))
          .addLast(new StreamingStatusLogger(atomicStream))

        channel.writeAndFlush(request)

      }
        .flatMap(_.toZIO)
        .flatMap { _ =>
          while (atomicStream.get().isEmpty)
            Thread.sleep(100)
          val status = atomicStream.get().get
          status.responseStream.run(ZSink.foreach(s => Console.printLine(s"[STREAMED] >>> $s"))) *> ZIO.succeed(
            status.statusCode
          )

        }
    }
  }

  def live                                                                                      = ZLayer.fromZIO {
    for {
      _       <- makeEventLoopGroup
      channel <- makeChannel()

    } yield channel
  }

  private def channelInitializer[A <: Channel]()                                                =
    new ChannelInitializer[A] {
      override def initChannel(ch: A): Unit = {
        ch.pipeline()
          .addLast(
            new LoggingHandler(getClass()),
            new HttpClientCodec(),
            new HttpContentDecompressor(),
            // new HttpObjectAggregator(Int.MaxValue)
          )

        ()
      }
    }

  private def makeKqueue(bootstrap: Bootstrap)                                                  = ZIO.acquireRelease(
    ZIO.attempt {
      val evlg = new KQueueEventLoopGroup(0, new DefaultThreadFactory("zio-zillen-kqueue"))

      bootstrap
        .group(evlg)
        .channel(classOf[KQueueDomainSocketChannel])
        .handler(channelInitializer[KQueueDomainSocketChannel]())
      evlg
    }
  )(evlg => ZIO.attemptBlocking(evlg.shutdownGracefully().get()).ignoreLogged)

  private def makeEpoll(bootstrap: Bootstrap)                                                   = ZIO.acquireRelease(
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

  private def makeChannel(path: String = "/var/run/docker.sock"): RIO[Bootstrap, DuplexChannel] = {
    val channel =
      ZIO.serviceWithZIO[Bootstrap] { bootstrap =>
        ZIO.attempt {
          bootstrap.connect(new DomainSocketAddress(path)).sync().channel()
        }
      }

    channel.flatMap {
      case c: DuplexChannel => ZIO.succeed(c)
      case other            => ZIO.fail(new Exception(s"Expected a duplex channel, got ${other.getClass.getName} instead."))
    }
  }

  private def makeEventLoopGroup: ZIO[Scope with Bootstrap, Throwable, EventLoopGroup]          =
    ZIO.serviceWithZIO[Bootstrap] { bootstrap =>
      if (Epoll.isAvailable())
        makeEpoll(bootstrap)
      else if (KQueue.isAvailable())
        makeKqueue(bootstrap)
      else
        ZIO.fail(new Exception("Could not create the appropriate event loop group.  Reason: OS not supported."))
    }

}
