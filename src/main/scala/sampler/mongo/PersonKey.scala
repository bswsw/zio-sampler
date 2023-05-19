package sampler.mongo

import zio.json.{JsonCodec, JsonDecoder, JsonEncoder}
import zio.prelude.Subtype

type PersonKey = PersonKey.Type
object PersonKey extends Subtype[String] {

  val enc: JsonEncoder[PersonKey] =
    JsonEncoder[String].contramap(_.value)

  val dec: JsonDecoder[PersonKey] =
    JsonDecoder[String].mapOrFail(PersonKey.make(_).toEitherWith(_.toString))

  given JsonCodec[PersonKey] = JsonCodec(enc, dec)

  extension (self: PersonKey) {
    def value: String = unwrap(self)
  }

}
