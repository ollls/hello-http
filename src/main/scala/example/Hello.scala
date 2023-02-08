package example

import zio.{ZIO, Chunk}
import zio.ZLayer
import zio.json._
import zio.stream.ZStream

import zhttp.{TcpServer, TLSServer, ContentType}
import zhttp.HttpRouter
import zhttp.HttpRoutes
import zhttp.dsl._
import zhttp.Response
import zhttp.Method._
import zhttp.{MultiPart, Headers}
import zio.logging.backend.SLF4J

object UserRecord {
  implicit val decoder: JsonDecoder[UserRecord] =
    DeriveJsonDecoder.gen[UserRecord]
  implicit val encoder: JsonEncoder[UserRecord] =
    DeriveJsonEncoder.gen[UserRecord]
}
case class UserRecord(val uid: String)

//Please see URL, for more examples/use cases.
//https://github.com/ollls/zio-tls-http/blob/master_zio2/examples/start/src/main/scala/MyServer.scala

object ServerExample extends zio.ZIOAppDefault {

  override val bootstrap =
    zio.Runtime.removeDefaultLoggers ++ SLF4J.slf4j ++ zio.Runtime.enableWorkStealing

  object param1 extends QueryParam("param1")

  def run = {

    val r = HttpRoutes.of {

      //works only for form-data provifing files.
      //files only!!!
      case req @ POST -> Root / "mpart2" => 
        for {
          _ <- MultiPart.writeAll( req, "/Users/ostrygun/tmp/" ) //last slash important!
        } yield(Response.Ok( ) )

      //exmple how to traverse all formdata in multipart 
      case req @ POST -> Root / "mpart" => {
        for {
          mpart_stream <- MultiPart.stream(req)
          _ <- mpart_stream.foreach {
            case h: Headers =>
              ZIO.debug("********************") *> ZIO.debug(
                "Headers: " + h.printHeaders
              )
            case b: Chunk[Byte] => ZIO.debug("Chunk Size = " + b.size)
          }
        } yield (Response.Ok())

      }

      // Chunked and ZStreams
      // Just an exercise how to send chunked data in compliaince with JSON array format.
      // each UserRecord is a separate HTTP Chunk with a separate TCP send.
      // You don't need to calculate content-len and you can serve the endless stream of records
      // it can be just a stream of effects, reading records one by one
      case GET -> Root / "chunked" =>
        ZIO.attempt {
          val zs = ZStream(
            UserRecord("user1"),
            UserRecord("user2"),
            UserRecord("user3")
          ).map(u => (u.toJson))
          val zs_first = zs.take(1).map(str => "[" + str) // prepend with [
          val zs_rest = zs
            .drop(1)
            .map(str => "," + str) // prepend all but first with commas
          val zs_result =
            zs_first ++ zs_rest ++ ZStream(
              "]"
            ) // last "]", this will be extra chunk 5 bytes lenght
          val zs_result_chunked =
            zs_result.map(str => Chunk.fromArray(str.getBytes()))

          Response
            .Ok()
            .asStream(zs_result_chunked)
            .contentType(ContentType.JSON)
            .transferEncoding("chunked")
        }

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
    ZIO.log(
      "See access log in access.log file, logback configuration in /resources"
    ) *>
      myHttpTLSServer.run(r) // .provideSomeLayer(AttributeLayer)
    // myHttpServer.run(r) // .provideSomeLayer(AttributeLayer)

  }
}
