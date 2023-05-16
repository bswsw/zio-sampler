package sampler.mongo

import com.mongodb.client.result.InsertOneResult
import mongo4cats.operations.Filter
import mongo4cats.zio.ZMongoDatabase
import mongo4cats.zio.json.*
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

case class PersonRepositoryImpl(db: ZMongoDatabase) extends PersonRepository {

  private val collection = db.getCollectionWithCodec[Person]("person")
  override def findByKey(key: String): Task[Option[Person]] = for {
    coll <- collection
    person <- coll.find(Filter.eq("key", key)).first
  } yield person

  override def save(person: Person): Task[String] = for {
    coll <- collection
    result <- coll.insertOne(person)
  } yield result.objectIdValue

  extension (self: InsertOneResult)
    def objectIdValue: String = self.getInsertedId.asObjectId().getValue.toString
}

object PersonRepositoryImpl {
  val layer: URLayer[ZMongoDatabase, PersonRepository] =
    ZLayer.fromFunction(PersonRepositoryImpl(_))
}
