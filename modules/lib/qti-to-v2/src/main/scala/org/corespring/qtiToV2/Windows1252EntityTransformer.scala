package org.corespring.qtiToV2

/**
 * The Windows-1252 character encoding is a superset of ISO 8859-1 which includes characters that are not properly
 * parsed into UTF-8. This trait includes a method that translates these characters into HTML entity declarations.
 */

object Windows1252EntityTransformer {

  val charCodeEntity = Map(
    8230 -> "hellip", 8364 -> "euro", 402 -> "fnof", 8216 -> "lsquo", 8217 -> "rsquo", 8220 -> "ldquo", 8221 -> "rdquo"
  )

  def transform(string: String): String = {
    charCodeEntity.foldLeft(string){ case (acc, (charCode, entity)) => {
      acc.replaceAll(charCode.toChar.toString, s"&$entity;")
    }}
  }

}
