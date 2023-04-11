package sampler

import zio.{Scope, ZIO, ZIOAppArgs, ZIOAppDefault, durationInt}

object ZIOOperatorSampleApp extends ZIOAppDefault {

  extension [R, E, A](zio: ZIO[R, E, A])
    def debugThread: ZIO[R, E, A] = zio.debug(s"[${Thread.currentThread()}]")

  private val logic1: Int => ZIO[Any, String, Int] =
    num => ZIO.succeed(num).delay(2.seconds).debugThread

  private val logic2: Int => ZIO[Any, String, Int] =
    num => ZIO.succeed(num * 2).debugThread

  private val program: Int => Int => ZIO[Any, String, (Int, Int)] =
    num1 => num2 => logic1(num1) <&> logic2(num2)

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = program(1)(2)
}
