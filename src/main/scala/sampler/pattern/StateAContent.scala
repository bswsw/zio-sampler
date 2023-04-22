package sampler.pattern

import zio.{IO, ZIO}

sealed trait StateAContent extends StateContent

object StateAContent {
  case class StartingAContent(num: Long) extends StateAContent {
    override def process(): IO[String, Int] = ???
  }

  case class ApprovingAContent(num: Long, title: String) extends StateAContent {
    override def process(): IO[String, Int] = ???
  }

  case class ApprovedAContent(num: Long, title: String, content: String) extends StateAContent {
    override def process(): IO[String, Int] = ???
  }

  def apply(content: ArticleContent.A): IO[String, StateAContent] = ZIO.fromEither(
    content match
      case ArticleContent.A(num, None, None)        => Right(StartingAContent(num))
      case ArticleContent.A(num, Some(title), None) => Right(ApprovingAContent(num, title))
      case ArticleContent.A(num, Some(title), Some(content)) =>
        Right(ApprovedAContent(num, title, content))
      case ArticleContent.A(_, None, Some(_)) => Left("invalid a content"),
  )
}
