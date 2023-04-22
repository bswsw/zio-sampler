package sampler.pattern

import sampler.pattern.ArticleStatus.*
import zio.{IO, ZIO}

sealed trait PreparingArticle {
  def process(): IO[String, Int]
}

object PreparingArticle {
  case class CreatedArticle(article: Article, stateContent: StateContent) extends PreparingArticle {
    override def process(): IO[String, Int] = stateContent.process()
  }

  case class TestingArticle(article: Article, stateContent: StateContent) extends PreparingArticle {
    override def process(): IO[String, Int] = stateContent.process()
  }

  case class ApprovingArticle(article: Article, stateContent: StateContent)
    extends PreparingArticle {
    override def process(): IO[String, Int] = stateContent.process()
  }

  case class ApprovedArticle(article: Article, stateContent: StateContent)
    extends PreparingArticle {
    override def process(): IO[String, Int] = stateContent.process()
  }

  def apply(article: Article): IO[String, PreparingArticle] = for {
    stateContent <- StateContent(article.content)
    article <- PreparingArticle(article, stateContent)
  } yield article

  private def apply(article: Article, stateContent: StateContent): IO[String, PreparingArticle] =
    ZIO.fromEither(
      article.status match
        case CREATED   => Right(CreatedArticle(article, stateContent))
        case TESTING   => Right(TestingArticle(article, stateContent))
        case APPROVING => Right(ApprovingArticle(article, stateContent))
        case APPROVED  => Right(ApprovedArticle(article, stateContent))
        case DELETED   => Left("불가능한 상태"),
    )
}
