package sampler.http

import zio.*
import zio.json.*

object EndpointSampleApp extends ZIOAppDefault {

  import zio.http.*
  import zio.http.codec.HttpCodec.*
  import zio.http.endpoint.Endpoint
  import zio.http.model.{Method, Status}

  //  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
  //    Runtime.removeDefaultLoggers >>> consoleJsonLogger()

  private val createPostEndpoint =
    Endpoint
      .post("posts")
      .out[PostCreator.Response]
      .outError[PostCreator.Error](Status.InternalServerError)

  private val createPostRoute =
    createPostEndpoint.implement(_ => PostCreator.program(PostCreator.Request("adasd", "asdasddas")))

  private val createPostApp = createPostRoute.toApp

  private val sampleApp = Http.collectZIO[Request] {
    case Method.GET -> !! / "posts" / id =>
      ZIO.succeed(Response.text(id))
  }

  private val server =
    Server
      .serve(sampleApp)
      .provide(Server.default)
      .debug

  private val program =
    PostCreator
      .program(PostCreator.Request("title", "content"))
      .debug

  override def run: ZIO[Any, Any, Any] = for {
    _ <- ZIO.logInfo("asdadad")
    _ <- server
  } yield ()
}

object PostCreator {

  import zio.schema.*

  import java.time.LocalDateTime

  def program(request: Request): IO[Error, Response] = for {
    _ <- ZIO.fail(Error("ERROR", "Invalid Title")).when(request.title.isEmpty)
    id <- Random.nextInt
    now <- Clock.localDateTime
  } yield Response(id, request.title, request.content, now)

  case class Request(title: String, content: String)

  object Request {
    given Schema[Request] = DeriveSchema.gen[Request]

    given JsonCodec[Request] = DeriveJsonCodec.gen[Request]
  }

  case class Response(id: Long, title: String, content: String, createdAt: LocalDateTime)

  object Response {
    given Schema[Response] = DeriveSchema.gen[Response]

    given JsonCodec[Response] = DeriveJsonCodec.gen[Response]
  }

  case class Error(code: String, message: String)

  object Error {
    given Schema[Error] = DeriveSchema.gen[Error]

    given JsonCodec[Error] = DeriveJsonCodec.gen[Error]
  }
}

