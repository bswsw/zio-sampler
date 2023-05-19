package sampler.mongo

import zio.json.*

import java.time.LocalDateTime

@jsonMemberNames(SnakeCase)
final case class Person(
  key: PersonKey,
  firstName: String,
  lastName: String,
  address: PersonAddress,
  createdAt: LocalDateTime,
)

object Person {
//  implicit val config: JsonCodecConfiguration = JsonCodecConfiguration(fieldNameMapping = SnakeCase)

//  implicit val enc: JsonEncoder[PersonKey] =
//    JsonEncoder[String].contramap(_.value)
//  implicit val dec: JsonDecoder[PersonKey] =
//    JsonDecoder[String].map(PersonKey(_))
//  implicit val codec: JsonCodec[Person] = DeriveJsonCodec.gen[Person]
}
