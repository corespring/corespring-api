package org.corespring.qtiToV2.utils

trait NumberParsers {

  private def safeParse[A](block: => A): Option[A] = try {
    Some(block)
  } catch {
    case n: NumberFormatException => None
  }

  def parseInt(string: String) = safeParse[Int] { string.toInt }
  def parseFloat(string: String) = safeParse[Float] { string.toFloat }

  def intValueOrZero(string: String): Int = safeParse[Int] { string.toInt }.getOrElse(0)
  def floatValueOrZero(string: String): Float = safeParse[Float] { string.toFloat }.getOrElse(0)

}