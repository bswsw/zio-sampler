package sampler.mongo

import mongo4cats.zio.json.*
import mongo4cats.zio.{ZMongoClient, ZMongoDatabase}
import zio.*

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object MongoSampleApp extends ZIOAppDefault {

  private val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

  private val client: TaskLayer[ZMongoClient] = ZLayer.scoped(
    ZMongoClient.fromConnectionString(
      "mongodb://root:example@localhost:27017/",
    ),
  )

  private val database: RLayer[ZMongoClient, ZMongoDatabase] = ZLayer.fromZIO(
    ZIO.serviceWithZIO[ZMongoClient](_.getDatabase("my-db")),
  )

  private val people = ZLayer.fromZIO(
    ZIO.serviceWithZIO[ZMongoDatabase](_.getCollectionWithCodec[Person]("person")),
  )

  private val program = for {
    now <- Clock.localDateTime
    num <- Random.nextInt
    key = s"KEY_${now.format(formatter)}_${num.abs}"
    _ <- PersonRepository
           .save(Person(key, "alan", "bae", LocalDateTime.now()))
           .debug("inserted id")
    person <- PersonRepository.findByKey(key).debug("person")
  } yield person

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    program.provide(client, database, people, PersonRepositoryImpl.layer)
}
