package sampler

import zio.{Scope, ZIO, ZIOAppArgs, ZIOAppDefault}

object ZIOFilterSampleApp extends ZIOAppDefault:
  private def program(logic: ZIO[Any, RuntimeException, String]) =
    logic
      .logError("1")
      .filterOrElse(_.contains("a"))(ZIO.fail(Error1()))
      .logError("2")
      .filterOrFail(_.contains("b"))(Error2())
      .logError("3")
      .filterOrElseWith(_.contains("c"))(ZIO.fail)
      .logError("4")
      .filterOrDie(_.contains("d"))(Error3())
      .logError("5")
      .filterOrDieMessage(_.contains("e"))("Not contain char: c")
      .logError("6")
      .filterOrDieWith(_.contains("f"))(Error4.apply)
      .logError("7")


  sealed abstract class CustomError(msg: String) extends RuntimeException(msg)

  case class Error1() extends CustomError("error1")

  case class Error2() extends CustomError("error2")

  case class Error3() extends CustomError("error3")
  case class Error4(msg: String) extends CustomError(s"error4: ${msg}")

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    program(ZIO.succeed("abcde")).debug("D E B U G")

