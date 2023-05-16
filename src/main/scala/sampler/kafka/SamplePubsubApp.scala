package sampler.kafka

import zio.*
import zio.kafka.consumer.*
import zio.kafka.serde.Serde
import zio.logging.backend.SLF4J
import zio.stream.{ZSink, ZStream}

object SamplePubsubApp extends ZIOAppDefault {

  private def commitOffsetBatches[K, V](
    commitRetryPolicy: Schedule[Any, Any, Any] =
      Schedule.exponential(1.second) && Schedule.recurs(3),
  ): ZSink[Any, Throwable, CommittableRecord[K, V], Nothing, Unit] =
    ZSink
      .foldLeft[CommittableRecord[K, V], OffsetBatch](OffsetBatch.empty)(_ add _.offset)
      .mapZIO(_.commitOrRetry(commitRetryPolicy))

  extension [R, E >: Throwable, K, V](self: ZStream[Consumer & R, E, CommittableRecord[K, V]])
    private def commitOffsetBatches: ZStream[Consumer & R, E, Unit] =
      self
        .map(a => a)
        .map(_.offset)
        .aggregateAsync(Consumer.offsetBatches)
        .mapZIO(_.commit)

  extension [R, E, A](self: ZIO[R, E, A])
    def debugRecord[K, V](implicit r: CommittableRecord[K, V]): ZIO[R, E, A] =
      self.debug(s"[partition: ${r.partition}, offset: ${r.offset.offset}]")

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers >>> SLF4J.slf4j

  private def process[K, V](implicit record: CommittableRecord[K, V]) = (for {
    _ <- ZIO.succeed(s"processing... ${record.value}").debugRecord
    _ <- ZIO.foreachDiscard(1 to 10) { n =>
           ZIO.succeed(s"${n}...").debugRecord.delay(1.seconds)
         }
    _ <- ZIO.succeed(s"processed... ${record.value}").debugRecord
  } yield record).onInterrupt(_ => ZIO.succeed(s"interrupted... ${record.value}...").debugRecord)

  private val consumer =
    Consumer
      .plainStream(
        Subscription.topics("test-topic"),
        Serde.string,
        Serde.string,
      )
      .mapZIOPar(2)(process)
      .aggregateAsync(commitOffsetBatches())

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
