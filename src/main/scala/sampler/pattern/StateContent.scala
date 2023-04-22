package sampler.pattern

import zio.IO

trait StateContent {
  def process(): IO[String, Int]
}

object StateContent {
  def apply(content: ArticleContent): IO[String, StateContent] = content match
    case a @ ArticleContent.A(num, title, content) => StateAContent(a)
    case b @ ArticleContent.B(title)               => StateBContent(b)
}
