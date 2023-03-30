package sampler.ops

import zio.*

trait EitherExtension {

  implicit class EitherOps[+E, +A](either: Either[E, A]) {
    def asZIO: IO[E, A] = ZIO.fromEither(either)
  }

  extension[E, A] (either: Either[E, A])
    def asZIO: IO[E, A] = ZIO.fromEither(either)

}
