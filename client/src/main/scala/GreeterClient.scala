import java.util.concurrent.TimeUnit

import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Try
import scala.util.control.NonFatal

import akka.Done
import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import io.grpc.CallOptions
import io.grpc.StatusRuntimeException
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts
import io.grpc.netty.shaded.io.grpc.netty.NegotiationType
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder

import io.grpc.examples._

object GreeterClient {

  def main(args: Array[String]): Unit = {

    val serverHost = "127.0.0.1"
    val serverPort = 8080

    val channelBuilder =
      NettyChannelBuilder
        .forAddress(serverHost, serverPort)
        .flowControlWindow(65 * 1024)
        .negotiationType(NegotiationType.PLAINTEXT)

    channelBuilder.overrideAuthority("127.0.0.1")

    val channel = channelBuilder.build()
    implicit val sys = ActorSystem()
    implicit val mat = ActorMaterializer()
    implicit val ec = sys.dispatcher
    try {

      val callOptions = CallOptions.DEFAULT

      val client =
        new io.grpc.examples.GreeterServiceClient(channel, callOptions)

      def singleRequestReply(): Unit = {
        val reply = client.sayHello(HelloRequest("Alice"))
        println(s"got single reply: ${Await.result(reply, 5.seconds).message}")
      }

      def streamingRequest(): Unit = {
        val requests = List("Alice", "Bob", "Peter").map(HelloRequest.apply)
        val reply = client.itKeepsTalking(Source(requests))
        println(s"got single reply for streaming requests: ${Await.result(reply, 5.seconds).message}")
      }


      def streamingReply(): Unit = {
        val responseStream = client.itKeepsReplying(HelloRequest("Alice"))
        val done: Future[Done] =
          responseStream.runForeach(reply => println(s"got streaming reply: ${reply.message}"))
        Await.ready(done, 1.minute)
      }

      def streamingRequestReply(): Unit = {
        val requestStream: Source[HelloRequest, NotUsed] =
          Source
            .tick(100.millis, 1.second, "tick")
            .zipWithIndex
            .map { case (_, i) => i }
            .map(i => HelloRequest(s"Alice-$i"))
            .take(10)
            .mapMaterializedValue(_ => NotUsed)

        val responseStream: Source[HelloReply, NotUsed] = client.streamHellos(requestStream)
        val done: Future[Done] =
          responseStream.runForeach(reply => println(s"got streaming reply: ${reply.message}"))
        Await.ready(done, 1.minute)
      }

      singleRequestReply()
      streamingRequest()
      streamingReply()
      streamingRequestReply()

    } catch {
      case e: StatusRuntimeException =>
        println(s"Status: ${e.getStatus}")
      case NonFatal(e) =>
        println(e)
    } finally {
      Try(channel.shutdown().awaitTermination(10, TimeUnit.SECONDS))
      sys.terminate()
    }

  }

}
