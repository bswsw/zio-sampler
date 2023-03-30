package sampler

import zio._

trait KeyValueStore[K, V, E, F[_, _]] {
  def get(key: K): F[E, V]

  def set(key: K, value: V): F[E, V]

  def del(key: K): F[E, Unit]
}

object KeyValueStore {
  def get[K: Tag, V: Tag, E: Tag](key: K) =
    ZIO.serviceWithZIO[KeyValueStore[K, V, E, IO]](_.get(key))

  def set[K: Tag, V: Tag, E: Tag](key: K, value: V) =
    ZIO.serviceWithZIO[KeyValueStore[K, V, E, IO]](_.set(key, value))

  def del[K: Tag, V: Tag, E: Tag](key: K) =
    ZIO.serviceWithZIO[KeyValueStore[K, V, E, IO]](_.del(key))
}

case class InmemoryKeyValueStore(map: Ref[Map[String, Int]]) extends KeyValueStore[String, Int, String, IO] {
  override def get(key: String): IO[String, Int] = map.get.map(_.get(key)).someOrFail(s"${key} not found")

  override def set(key: String, value: Int): IO[String, Int] = map.update(_.updated(key, value)).as(value)

  override def del(key: String): IO[String, Unit] = map.update(_.removed(key))
}

object InmemoryKeyValueStore {
  val live: ZLayer[Any, Nothing, InmemoryKeyValueStore] = ZLayer {
    Ref.make(Map.empty[String, Int]).map(InmemoryKeyValueStore(_))
  }
}

object MainApp extends ZIOAppDefault {

  implicit class ZIOOps[-R, +E, +A](zio: ZIO[R, E, A]) {
    def debugThread: ZIO[R, E, A] = zio.debug(s"[${Thread.currentThread().getName}]")
  }

  val program = for {
    _ <- KeyValueStore.set[String, Int, String]("hello", 1).debugThread
    _ <- KeyValueStore.get[String, Int, String]("hello").debugThread
    _ <- KeyValueStore.del[String, Int, String]("hello").debugThread
  } yield ()

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    program.provide(InmemoryKeyValueStore.live)
}