package sampler.period

import sampler.period
import sampler.period.DayOfMonth.{unwrap, wrap}
import zio.prelude.{Assertion, Newtype}

type DayOfMonth = DayOfMonth.Type

object DayOfMonth extends Newtype[Int] {
  self =>

  import Assertion.*

  val first: DayOfMonth = DayOfMonth(1)

  override inline def assertion: Assertion[Int] =
    greaterThanOrEqualTo(1) && lessThanOrEqualTo(31)

  extension (self: DayOfMonth)
    infix def add(that: DayOfMonth): DayOfMonth = wrap(unwrap(self) + unwrap(that))
    def minus(i: Int): DayOfMonth = wrap(unwrap(self) - i)
    def isFirst: Boolean = self == DayOfMonth(1)
}
