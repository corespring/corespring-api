package common.utils.string

import common.utils.string
import util.matching.Regex

object StringUtils {

  @deprecated("use common.utils.string instead", "")
  val DefaultRegex = string.DefaultRegex

  @deprecated("use common.utils.string instead", "")
  val DollarRegex = string.DollarRegex

  @deprecated("use common.utils.string instead", "")
  def interpolate(text: String, lookup: String => String, regex : Regex = DefaultRegex )  = string.interpolate(text, lookup, regex)

  @deprecated("use common.utils.string instead", "")
  def replaceKey(tokens: Map[String,String])(s: String): String = string.replaceKey(tokens)(s)
}

