package sampler.mongo

import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

case class PersonAddress(city: String, country: String)

object PersonAddress {
  implicit val dec: JsonDecoder[PersonAddress] = DeriveJsonDecoder.gen
  implicit val enc: JsonEncoder[PersonAddress] = DeriveJsonEncoder.gen
}
