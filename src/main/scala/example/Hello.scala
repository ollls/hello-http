package example

import zio.ZIO
import zio.ZLayer
import zio.json._

import zhttp.{TcpServer, TLSServer}
import zhttp.HttpRouter
import zhttp.HttpRoutes
import zhttp.dsl._
import zhttp.Response
import zhttp.Method._
import zio.logging.backend.SLF4J

object UserRecord {
  implicit val decoder: JsonDecoder[UserRecord] =
    DeriveJsonDecoder.gen[UserRecord]
  implicit val encoder: JsonEncoder[UserRecord] =
    DeriveJsonEncoder.gen[UserRecord]
}
case class UserRecord(val uid: String)

//Please see URL, for more examples/use cases.
//https://github.com/ollls/zio-tls-http/blob/dev/examples/start/src/main/scala/MyServer.scala

/*
   How to switch to secure channel

   import zhttp.TLSServer
   val myHttp = new TLSServer[MyEnv]( port = 8443,
                                      keepAlive = 4000,
                                      serverIP = "0.0.0.0",
                                      keystore = "keystore.jks", "password",
                                      tlsVersion = "TLSv1.2" )
 */

object ServerExample extends zio.ZIOAppDefault {

  override val bootstrap =
    zio.Runtime.removeDefaultLoggers ++ SLF4J.slf4j ++ zio.Runtime.enableWorkStealing

  object param1 extends QueryParam("param1")

  def run = {

    val r = HttpRoutes.of {
      case GET -> Root / "health" =>
        ZIO.attempt(Response.Ok().asTextBody("Health Check Ok"))

      case GET -> Root / "user" :? param1(par) =>
        ZIO.attempt(Response.Ok().asJsonBody(UserRecord(par)))

      // Expected POST body: { "uid" :  "username" }
      case req @ POST -> Root / "test" =>
        for {
          rec <- req.fromJSON[UserRecord]
          _ <- ZIO.log("UID received: " + rec.uid)
        } yield (Response.Ok().asTextBody("OK " + rec.uid))
    }

    val myHttpServer =
      new TcpServer[Any](port = 8080, keepAlive = 2000, serverIP = "0.0.0.0")

    val myHttpTLSServer = new TLSServer[Any](
      port = 8084,
      keepAlive = 4000,
      serverIP = "0.0.0.0",
      // serverIP = "127.0.0.1",
      keystore = "keystore.jks",
      "password",
      tlsVersion = "TLSv1.2"
    )

    val myHttpRouter = new HttpRouter[Any](r)

    // environment now commented out
    // val AttributeLayer = ZLayer.fromZIO(ZIO.succeed("flag#1-1"))

    // port 8080 plain and port 8084 encrypted
    // to run unencrypted connection uncomment non-tls, and comment out non tls
    myHttpTLSServer.run(r) // .provideSomeLayer(AttributeLayer)
    // myHttpServer.run(r) // .provideSomeLayer(AttributeLayer)

  }
}
