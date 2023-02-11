# hello-http
to run: sbt run

Server code:
https://github.com/ollls/hello-http/blob/main/src/main/scala/example/Hello.scala

Shorter example:
```scala
package example
import zio.logging.backend.SLF4J
import zio.{ZIO, Chunk}
import zhttp.Method._
import zhttp.dsl._
import zhttp.{TLSServer, TcpServer, HttpRoutes}
import zhttp.{MultiPart, Headers, ContentType, Response, FileUtils}

object MyApp extends zio.ZIOAppDefault {

  override val bootstrap =
    zio.Runtime.removeDefaultLoggers ++ SLF4J.slf4j ++ zio.Runtime.enableWorkStealing
    
  val r = HttpRoutes.of { case GET -> Root / "health" =>
    ZIO.attempt(Response.Ok().asTextBody("Health Check Ok"))
  }
  val myHttp =
    new TcpServer[Any](port = 8080, keepAlive = 2000, serverIP = "0.0.0.0")
  val run = myHttp.run(r)
}

```
To enable more detailed logging, use logback-test.xml with "debug" or "trace" levels

```xml
  <root level="debug">
    <appender-ref ref="STDOUT" />
  </root>
```



