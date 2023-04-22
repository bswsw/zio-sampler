package sampler.kafka

import zio.*
import zio.kafka.consumer.*
import zio.kafka.serde.Serde
import zio.logging.backend.SLF4J
import zio.stream.{ZSink, ZStream}

object SamplePubsubApp extends ZIOAppDefault {

  def commitOffsetBatches[K, V]: ZSink[Any, Throwable, CommittableRecord[K, V], Nothing, Unit] =
    ZSink
      .foldLeft[CommittableRecord[K, V], OffsetBatch](OffsetBatch.empty)(_ add _.offset)
      .mapZIO(_.commit)

  extension [R, E >: Throwable, K, V](self: ZStream[Consumer & R, E, CommittableRecord[K, V]])
    private def commitOffsetBatches: ZStream[Consumer & R, E, Unit] =
      self
        .map(a => a)
        .map(_.offset)
        .aggregateAsync(Consumer.offsetBatches)
        .mapZIO(_.commit)

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers >>> SLF4J.slf4j

  private val consumer =
    Consumer
      .plainStream(
        Subscription.topics("test-topic"),
        Serde.string,
        Serde.string,
      )
      .tap(r => ZIO.logInfo(s"[partition: ${r.partition}, offset: ${r.offset.offset}] ${r.value}"))
      .aggregateAsync(commitOffsetBatches)

  private val consumerLayer = ZLayer.scoped(
    Consumer.make(
      ConsumerSettings(List("localhost:9092"))
        .withGroupId("test-group"),
    ),
  )

  private val program =
    consumer.runDrain

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    program.provide(consumerLayer)
}
