package common.seed

import util.matching.Regex

object StringUtils {

  def interpolate(text: String, lookup: String => String, regex : Regex = """\$\[interpolate\{([^}]+)\}\]""".r ) =
    regex.replaceAllIn(text, (_: scala.util.matching.Regex.Match) match {
      case Regex.Groups(v) => {
        val result = lookup(v)
        result
      }
    })
}

