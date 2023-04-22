package sampler.http

import sampler.ops.EitherExtension
import zio.*
import zio.http.*

trait HttpClient {
  def findData(data: String): IO[Throwable, String]
}

object HttpClient {
  private val httpClient = ZIO.serviceWithZIO[HttpClient]

  def findData(data: String): ZIO[HttpClient, Throwable, String] =
    httpClient(_.findData(data))
}

case class HttpClientImpl(client: Client) extends HttpClient with EitherExtension {
  override def findData(data: String): IO[Throwable, String] = for {
    url <- URL.decode(s"http://localhost:8080/${data}?param1=1").asZIO
    _ <- ZIO.logError(url.encode)
    response <- client.request(Request.get(url))
    body <- response.body.asString
  } yield body
}

object HttpClientImpl {
  def live: ZLayer[Client, Nothing, HttpClient] =
    ZLayer.fromFunction(HttpClientImpl(_))
}
