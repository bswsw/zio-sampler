package sampler.period

import zio.*

object PeriodApp extends ZIOAppDefault {
  override def run: ZIO[Environment & ZIOAppArgs & Scope, Any, Any] = for {
    _ <- toDay(1).debug
    _ <- toDay(111).debug
  } yield ()

  private def toDay(i: Int) = DayOfMonth.make(i).toZIO
}
