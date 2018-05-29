import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import akka.actor.ActorSystem
import akka.http.scaladsl._
import akka.http.scaladsl.model._
// import akka.http.scaladsl.UseHttp2.Always
import akka.stream.ActorMaterializer
import akka.stream.Materializer
import com.typesafe.config.ConfigFactory

object GreeterServer {

  def main(args: Array[String]): Unit = {
    // important to enable HTTP/2 in ActorSystem's config
    val conf = ConfigFactory.parseString("akka.http.server.preview.enable-http2 = on")
      .withFallback(ConfigFactory.defaultApplication())
    implicit val sys: ActorSystem = ActorSystem("HelloWorld", conf)
    implicit val mat: Materializer = ActorMaterializer()
    implicit val ec: ExecutionContext = sys.dispatcher

    val service: HttpRequest => Future[HttpResponse] =
      io.grpc.examples.GreeterServiceHandler(new GreeterServiceImpl(mat))

    // Http2().bindAndHandleAsync(
    //   service,
    //   interface = "127.0.0.1",
    //   port = 8080,
    //   connectionContext = HttpConnectionContext(http2 = Always)
    // )
    Http2().bindAndHandleAsync(
      service,
      interface = "127.0.0.1",
      port = 8080,
      httpsContext = new HttpsConnectionContext(javax.net.ssl.SSLContext.getDefault())
    )
    // ,
    //   connectionContext = HttpConnectionContext(http2 = Always)
    // )
    .foreach { binding =>
      println(s"GRPC server bound to: ${binding.localAddress}")
    }
  }

}
