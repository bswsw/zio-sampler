package sampler.http

import zio.ZIO
import zio.http.*
import zio.metrics.connectors.prometheus.PrometheusPublisher

object PrometheusPublisherApp {

  val routes: Http[PrometheusPublisher, Nothing, Request, Response] = Http.collectZIO[Request] {
    case Method.POST -> !! / "metrics" =>
      ZIO.serviceWithZIO[PrometheusPublisher](_.get.map(Response.text))
  }

}
