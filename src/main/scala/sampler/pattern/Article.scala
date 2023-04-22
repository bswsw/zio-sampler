package sampler.pattern

final case class Article(id: ArticleId, content: ArticleContent, status: ArticleStatus)

final class ArticleId(val value: String) extends AnyVal

sealed trait ArticleContent

object ArticleContent {
  case class A(num: Long, title: Option[String], content: Option[String]) extends ArticleContent

  case class B(title: Option[String]) extends ArticleContent
}

enum ArticleStatus {
  case CREATED, TESTING, APPROVING, APPROVED, DELETED
}
