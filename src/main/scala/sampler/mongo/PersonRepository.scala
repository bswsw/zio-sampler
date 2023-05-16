package sampler.mongo

import com.mongodb.client.result.InsertOneResult
import mongo4cats.operations.Filter
import mongo4cats.zio.ZMongoCollection
import zio.*

trait PersonRepository {
  def findByKey(key: String): Task[Option[Person]]

  def save(person: Person): Task[String]
}

object PersonRepository {
  def findByKey(key: String): ZIO[PersonRepository, Throwable, Option[Person]] =
    ZIO.serviceWithZIO[PersonRepository](_.findByKey(key))

  def save(person: Person): ZIO[PersonRepository, Throwable, String] =
    ZIO.serviceWithZIO[PersonRepository](_.save(person))
}

case class PersonRepositoryImpl(people: ZMongoCollection[Person]) extends PersonRepository {

  override def findByKey(key: String): Task[Option[Person]] =
    people.find(Filter.eq("key", key)).first

  override def save(person: Person): Task[String] =
    people.insertOne(person).map(_.objectIdValue)

  extension (self: InsertOneResult)
    def objectIdValue: String = self.getInsertedId.asObjectId().getValue.toString
}

object PersonRepositoryImpl {
  val layer: URLayer[ZMongoCollection[Person], PersonRepository] =
    ZLayer.fromFunction(PersonRepositoryImpl(_))
}
