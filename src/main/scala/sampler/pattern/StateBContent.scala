package sampler.pattern

import zio.{IO, ZIO}

sealed trait StateBContent extends StateContent

object StateBContent {
  case class TestingBContent() extends StateBContent {
    override def process(): IO[String, Int] = ???
  }

  case class ApprovedBContent(title: String) extends StateBContent {
    override def process(): IO[String, Int] = ???
  }

  def apply(content: ArticleContent.B): IO[Nothing, StateBContent] = ZIO.fromEither(
    content match
      case ArticleContent.B(None)        => Right(TestingBContent())
      case ArticleContent.B(Some(title)) => Right(ApprovedBContent(title)),
  )
}
