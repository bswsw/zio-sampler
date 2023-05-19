package sampler.mongo

import zio.json.{JsonCodec, JsonDecoder, JsonEncoder}

case class PersonKey2(v: String)

object PersonKey2 {
  val enc: JsonEncoder[PersonKey2] =
    JsonEncoder[String].contramap(_.v)
  val dec: JsonDecoder[PersonKey2] =
    JsonDecoder[String].mapOrFail(a => Right(PersonKey2(a)))

  given JsonCodec[PersonKey2] = JsonCodec(enc, dec)
}
