package sampler.mongo

import zio.json.*

import java.time.LocalDateTime

@jsonMemberNames(SnakeCase)
final case class Person(key: String, firstName: String, lastName: String, createdAt: LocalDateTime)

object Person {
//  implicit val config: JsonCodecConfiguration = JsonCodecConfiguration(fieldNameMapping = SnakeCase)
  implicit val codec: JsonCodec[Person] = DeriveJsonCodec.gen[Person]
}
