package sampler.period

import java.time.{DayOfWeek, LocalTime}

sealed trait PlanPreparationPeriod

object PlanPreparationPeriod {
  case class MONTHLY(day: DayOfMonth, time: LocalTime) extends PlanPreparationPeriod

  case class WEEKLY(dayOfWeek: DayOfWeek, time: LocalTime) extends PlanPreparationPeriod

  case class DAILY(time: LocalTime) extends PlanPreparationPeriod

  case class LAST_DAY_OF_MONTH(time: LocalTime) extends PlanPreparationPeriod

  case object None extends PlanPreparationPeriod
}
