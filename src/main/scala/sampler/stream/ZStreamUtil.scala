package sampler.stream

import zio.ZIO
import zio.stream.ZPipeline

object ZStreamUtil {
  type ThrowableEither[+T] = Either[Throwable, T]

  def eitherPipeZIO[Env, In, Out](f: In => ZIO[Env, Throwable, Out])(using parallelism: Int): ZPipeline[Env, Nothing, ThrowableEither[In], ThrowableEither[Out]] =
    ZPipeline.mapZIOParUnordered[Env, Nothing, ThrowableEither[In], ThrowableEither[Out]](parallelism) { either =>
      ZIO
        .fromEither(either)
        .flatMap(f)
        .either
    }

  def eitherPipe[In, Out](f: In => ThrowableEither[Out]): ZPipeline[Any, Nothing, In, ThrowableEither[Out]] =
    ZPipeline.map[In, ThrowableEither[Out]](f)
}
