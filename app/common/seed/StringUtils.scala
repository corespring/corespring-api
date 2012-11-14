package common.seed

import util.matching.Regex

object StringUtils {

  val DefaultRegex = """\$\[interpolate\{([^}]+)\}\]""".r

  def interpolate(text: String, lookup: String => String, regex : Regex = DefaultRegex )  =
    regex.replaceAllIn(text, (_: scala.util.matching.Regex.Match) match {
      case Regex.Groups(v) => {
        val result = lookup(v)
        result
      }
    })
}

