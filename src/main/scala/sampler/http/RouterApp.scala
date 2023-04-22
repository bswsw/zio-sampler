package sampler.http

import zio.*
import zio.http.*
import zio.logging.backend.SLF4J
import zio.metrics.connectors.{MetricsConfig, prometheus}

object RouterApp extends ZIOAppDefault {

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers >>> SLF4J.slf4j

  private val router = Http.collectZIO[Request] { case Method.GET -> !! / "sample" / path =>
    for {
      calculator <- ZIO.service[Calculator]
      result <- calculator.cal(10, path.toInt)
    } yield Response.text(result.toString)
  }

  private val metricsConfigLayer = ZLayer.succeed(MetricsConfig(1.seconds))

  private val app = (router @@ LoggingMiddleware.logging).withDefaultErrorResponse

  private val server = Server.serve(app ++ PrometheusPublisherApp.routes)

  override def run: ZIO[Environment & ZIOAppArgs & Scope, Any, Any] =
    server.provide(
      ZLayer.succeed(Server.Config.default),
      Server.live,
      PlusCalculator.live,
      prometheus.publisherLayer,
      prometheus.prometheusLayer,
      metricsConfigLayer,
    )
}

trait Calculator {
  def cal(x: Int, y: Int): ZIO[Any, Throwable, Int]
}

case class PlusCalculator() extends Calculator {
  override def cal(x: Int, y: Int): ZIO[Any, Throwable, Int] = ZIO.attempt(x + y)
}

object PlusCalculator {
  val live: ULayer[PlusCalculator] = ZLayer.succeed(PlusCalculator())
}
