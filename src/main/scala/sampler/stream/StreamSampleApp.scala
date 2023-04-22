package sampler.stream

import sampler.stream.ZStreamUtil.ThrowableEither
import zio.*
import zio.stream.*

object StreamSampleApp extends ZIOAppDefault {

  private given parallelism: Int = 10

  private val source =
    ZStream(1, 2, 3, 4, 5, 6, 0, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20)

  private val plusPipeZIO =
    ZPipeline.mapZIOParUnordered[Any, Nothing, Int, ThrowableEither[Int]](parallelism) { num =>
      ZIO.attempt(num + 2).either
    }

  private val minusPipeZIO = ZStreamUtil.eitherPipeZIO[Any, Int, Int] { num =>
    ZIO.attempt(num - 2)
  }

  private val productPipeZIO = ZStreamUtil.eitherPipeZIO[Any, Int, Int] { num =>
    ZIO.attempt(num * 3)
  }

  private val dividePipeZIO = ZStreamUtil.eitherPipeZIO[Any, Int, Int] { num =>
    ZIO.attempt(100 / num)
  }

  private val resultSink: ZSink[Any, Nothing, ThrowableEither[Int], Nothing, Result] =
    ZSink
      .collectAll[ThrowableEither[Int]]
      .map(_.partitionMap(identity))
      .map(Result(_))

  private val program: ZIO[Any, Nothing, Result] =
    source >>>
      plusPipeZIO >>>
      dividePipeZIO >>>
      minusPipeZIO >>>
      productPipeZIO >>>
      dividePipeZIO >>>
      dividePipeZIO >>>
      dividePipeZIO >>>
      resultSink

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    program.debug

  case class Result(errors: List[String], nums: List[Int]) {
    def toCount: (RuntimeFlags, RuntimeFlags) = (errors.size, nums.size)
  }

  object Result {
    def apply(tuple: (Chunk[Throwable], Chunk[Int])): Result =
      val (a, b) = tuple
      Result(a.toList.map(_.getMessage), b.toList)
  }
}
