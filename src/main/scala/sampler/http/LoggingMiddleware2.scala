package sampler.http

import zio.*

//import zio.Clock
//import zio.http.HttpAppMiddleware
//
//import java.time.temporal.ChronoUnit
//
//object LoggingMiddleware2 {
//  def logging[R, E, S] = HttpAppMiddleware.interceptPatchZIO[R, E, S] { request =>
//      Clock.currentTime(ChronoUnit.MILLIS).map((request, _))
//    } { case (response, (request, startTime)) =>
//      Clock.currentTime(ChronoUnit.MILLIS).flatMap { endTime =>
//
//      }
//    }
//}

object LoggingMiddleware2 extends ZIOAppDefault {

  extension [R, E, A](self: ZIO[R, E, A])
    def timedMilli: ZIO[R, E, (Long, A)] =
      self.timed.map { case (duration, response) =>
        (duration.toMillis, response)
      }
  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    ZIO
      .succeed("asddadsda")
      .delay(5.seconds)
      .timedMilli
      .debug
}
