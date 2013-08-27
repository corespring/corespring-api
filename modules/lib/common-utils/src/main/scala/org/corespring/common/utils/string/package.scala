package org.corespring.common.utils

import scala.util.matching.Regex

package object string {

  val DefaultRegex = """\$\[interpolate\{([^}]+)\}\]""".r

  val DollarRegex = """\$\{([^}]+)\}""".r

  def interpolate(text: String, lookup: String => String, regex: Regex = DefaultRegex) =
    regex.replaceAllIn(text, (_: scala.util.matching.Regex.Match) match {
      case Regex.Groups(v) => {
        val result = lookup(v)
        result
      }
    })

  def replaceKey(tokens: Map[String, String])(s: String): String = tokens.getOrElse(s, "?")

  def lowercaseFirstChar(s: String): String = if (s == null || s.isEmpty) s else s.charAt(0).toLower + s.substring(1, s.length)

  def filePath(parts: String*): String = {
    parts.mkString("/").replace("//", "/").replace("/./", "/")
  }

  def pseudoRandomString(length: Int, chars: Seq[Char] = ('a' to 'z') ++ ('A' to 'Z')): String = {
    (1 to length).map(
      x => {
        val index = scala.util.Random.nextInt(chars.length)
        chars(index)
      }).mkString("")
  }

}
