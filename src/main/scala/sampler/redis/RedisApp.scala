package sampler.redis

import zio.*
import zio.redis.*
import zio.redis.embedded.*
import zio.schema.Schema
import zio.schema.codec.{BinaryCodec, JsonCodec}

object RedisApp extends ZIOAppDefault {

  private val program = for {
    redis <- ZIO.service[Redis]
    result <- redis.get("asdsadsa").returning[String]
  } yield result

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    program
      .provide(
        EmbeddedRedis.layer,
        RedisExecutor.layer,
        Redis.layer,
        ZLayer.succeed(
          new CodecSupplier {
            override def get[A: Schema]: BinaryCodec[A] = JsonCodec.schemaBasedBinaryCodec
          },
        ),
      )
      .debug

}
