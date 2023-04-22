package sampler

import zio.*

import java.io.IOException

object ErrorHandlingApp extends ZIOAppDefault {

  // 1
  val aBadFailure = ZIO.succeed[Int](throw MyException())
  val aBetterFailure: ZIO[Any, Cause[Nothing], RuntimeFlags] = aBadFailure.sandbox
  val aBetterFailure_v2: ZIO[Any, Throwable, RuntimeFlags] = aBadFailure.unrefine { case e =>
    e
  }
  val aBetterFailure_v3 = aBetterFailure.foldZIO(
    a => ZIO.succeed("error"),
    b => ZIO.succeed("true"),
  )

  // 2
  def ioException[R, A](zio: ZIO[R, Throwable, A]): ZIO[R, IOException, A] =
    zio.refineOrDie { case ioe: IOException =>
      ioe
    }

  def ioException_v2[R, A](zio: ZIO[R, Throwable, A]): ZIO[R, IOException, A] =
    zio.refineToOrDie[IOException]

  // 3
  def left[R, E, A, B](zio: ZIO[R, E, Either[A, B]]): ZIO[R, Either[E, A], B] =
    zio.foldZIO(
      e => ZIO.fail(Left(e)),
      either =>
        either match {
          case Left(value)  => ZIO.fail(Right(value))
          case Right(value) => ZIO.succeed(value)
        },
    )

  // 4
  def database = Map(
    "daniel" -> 123,
    "alice" -> 789,
  )

  case class QueryError(reason: String)

  case class UserProfile(name: String, phone: Int)

  def lookupProfile(userId: String): ZIO[Any, QueryError, Option[UserProfile]] =
    if (userId != userId.toLowerCase())
      ZIO.fail(QueryError(s"invalid userId: ${userId}"))
    else
      ZIO.succeed(database.get(userId).map(phone => UserProfile(userId, phone)))

  def betterLookupProfile(userId: String): ZIO[Any, Option[QueryError], UserProfile] =
    lookupProfile(userId).foldZIO(
      e => ZIO.fail(Some(e)),
      profileOpt =>
        profileOpt match {
          case Some(value) => ZIO.succeed(value)
          case None        => ZIO.fail(None)
        },
    )

  def betterLookupProfile_v2(userId: String): ZIO[Any, Option[QueryError], UserProfile] =
    lookupProfile(userId).some

  case class MyException() extends RuntimeException

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = (for {
    d <- aBetterFailure_v3.debug
    a <- ZIO.succeed("dgfd").debug
  } yield (d, a))
}
