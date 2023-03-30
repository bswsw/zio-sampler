package sampler.period

import sampler.period

import java.time.{DayOfWeek, LocalDateTime, LocalTime}

sealed trait PlanExecutionPeriod {
  def toPreparationPeriod: PlanPreparationPeriod
  def toExecutionExpectedAt: LocalDateTime
}

// 2023-03-30 11:30:00

def timeTo(dateTime: LocalDateTime) = {
  val executionExpectedAt = dateTime.plusDays(1)
  val executionExpectedDate = executionExpectedAt.toLocalDate
  val executionExpectedTime = executionExpectedAt.toLocalTime

  val dayOfMonth = DayOfMonth.make(executionExpectedAt.getDayOfMonth)

  val monthly = PlanExecutionPeriod.MONTHLY(dayOfMonth.get, executionExpectedTime)
  val weekly = PlanExecutionPeriod.WEEKLY(dateTime.getDayOfWeek.plus(1), executionExpectedTime)
  val daily = PlanExecutionPeriod.DAILY(executionExpectedTime)

}



object PlanExecutionPeriod {
  case class MONTHLY(day: DayOfMonth, time: LocalTime) extends PlanExecutionPeriod {
    override def toPreparationPeriod: PlanPreparationPeriod =
      if (day.isFirst) {
        PlanPreparationPeriod.LAST_DAY_OF_MONTH(time)
      } else {
        PlanPreparationPeriod.MONTHLY(day.minus(1), time)
      }

    // 매월 5일 11시 30분
    override def toExecutionExpectedAt: LocalDateTime = ???
  }

  case class WEEKLY(dayOfWeek: DayOfWeek, time: LocalTime) extends PlanExecutionPeriod {
    override def toPreparationPeriod: PlanPreparationPeriod = PlanPreparationPeriod.WEEKLY(dayOfWeek.minus(1), time)

    override def toExecutionExpectedAt: LocalDateTime = ???
  }

  case class DAILY(time: LocalTime) extends PlanExecutionPeriod {
    override def toPreparationPeriod: PlanPreparationPeriod = PlanPreparationPeriod.DAILY(time)

    override def toExecutionExpectedAt: LocalDateTime = ???
  }

  case class LAST_DAY_OF_MONTH(time: LocalTime) extends PlanExecutionPeriod {
    override def toPreparationPeriod: PlanPreparationPeriod = ???

    override def toExecutionExpectedAt: LocalDateTime = ???
  }

  case object None extends PlanExecutionPeriod {
    override def toPreparationPeriod: PlanPreparationPeriod = PlanPreparationPeriod.None

    override def toExecutionExpectedAt: LocalDateTime = ???
  }
}
