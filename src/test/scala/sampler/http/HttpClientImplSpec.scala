package sampler.http

import zio._
import zio.http._
import zio.test.Assertion._
import zio.test._

object HttpClientImplSpec extends ZIOSpecDefault {

  private val suites =
    suite("test")(
      test("1 + 2") {
        assertTrue(1 + 2 == 3)
      },

      test("http client") {
        val actual = for {
          data <- ZIO.succeed("adadada")
          url <- ZIO.fromEither(URL.fromString(s"http://localhost:8080/$data"))
          _ <- TestClient.addRequestResponse(Request.get(url), Response.text("abcd"))
          sut <- ZIO.service[HttpClient]
          actual <- sut.findData(data)
        } yield actual

        assertZIO(actual)(equalTo("abcd"))
      }
    ) @@ TestAspect.debug

  override def spec: Spec[TestEnvironment with Scope, Any] =
    suites.provide(TestClient.layer, HttpClientImpl.live)
}


object MyAspect {
  def add[T] =
    new ZIOAspect[Any, Any, String, String, T, Any] {
      override def apply[R >: Any <: Any, E >: String <: String, A >: T <: Any](zio: ZIO[R, E, A])(implicit trace: Trace): ZIO[R, E, A] =
        zio.debug
    }
}


object MyApp extends ZIOAppDefault {

  val program = for {
    a <- ZIO.succeed(1)
    b <- ZIO.succeed(2)
    c <- ZIO.succeed(3)
    d <- ZIO.succeed(4)
    aa <- ZIO.succeed("A")
    bb <- ZIO.succeed("B")
    cc <- ZIO.succeed("C")
    dd <- ZIO.succeed("D")
  } yield (a, b, c, d, aa, bb, cc, dd)

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = {
    val aspect: ZIOAspect[Any, Any, String, String, Nothing, Any] = MyAspect.add
    val composed = program @@ aspect

    for {
      a <- composed
      _ <- ZIO.logInfo(s"a: ${a}")
    } yield ()
  }
}