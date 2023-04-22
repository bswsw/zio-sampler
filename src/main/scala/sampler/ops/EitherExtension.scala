package sampler.ops

import zio.*

trait EitherExtension {

  extension [E, A](either: Either[E, A])
    def asZIO: IO[E, A] =
      ZIO.fromEither(either)

}
