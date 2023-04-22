package sampler.http

import zio.*
import zio.http.*

object LoggingMiddleware {

  def logging[R, E >: Throwable]: RequestHandlerMiddleware.Simple[R, E] =
    new RequestHandlerMiddleware.Simple[R, E]:
      override def apply[Env <: R, Err >: E](
        handler: Handler[Env, Err, Request, Response],
      )(implicit trace: Trace): Handler[Env, Err, Request, Response] =
        Handler.fromFunctionZIO[Request] { request =>
          handler
            .runZIO(request)
            .foldZIO(
              e => ZIO.succeed(Response.text(e.toString)),
              a => internal(request, a),
            )
        }

      private def internal(request: Request, response: Response): ZIO[R, E, Response] = for {
        requestBody <- request.body.asString
        responseBody <- response.body.asString
        _ <- ZIO.logInfo(
               s"""
           |req: ${requestBody}
           |res: ${responseBody}
           |""".stripMargin,
             )
      } yield response

}
