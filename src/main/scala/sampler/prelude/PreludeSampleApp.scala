package sampler.prelude

import zio.*
import zio.json.*
import zio.prelude.*

object PreludeSampleApp extends ZIOAppDefault {

  def getStr(str: String) = ZIO.logInfo(str)

  val program = for {
    _ <- getStr(SubString("a"))
    _ <- ZIO.logInfo(s"bbb: ${SubString("a")}")
  } yield ()

  val program2 = for {
    _ <- ZIO.fromEither(""""add"""".fromJson[SubString]).debug
    _ <- ZIO.attempt(SubString("abc").toJson).debug
  } yield ()
  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = program2
}

object SubString extends Subtype[String] {

  import Assertion.*
  override inline def assertion: Assertion[String] = hasLength(between(1, 3))

  given JsonEncoder[SubString] =
    JsonEncoder[String].contramap(identity)
  given JsonDecoder[SubString] =
    JsonDecoder[String].mapOrFail(SubString.make(_).toEitherWith(_.mkString(",")))
}
type SubString = SubString.Type

object NewString extends Newtype[String] { self =>

  import Assertion.*

  extension (self: NewString) {
    def value: String = unwrap(self)
  }
  override inline def assertion: Assertion[String] = hasLength(between(1, 3))

  given jsonEncoder: JsonEncoder[NewString] =
    JsonEncoder[String].contramap(_.value)

  given jsonDecoder: JsonDecoder[NewString] =
    JsonDecoder[String].mapOrFail(NewString.make(_).toEitherWith(_.mkString(",")))

}
type NewString = NewString.Type
