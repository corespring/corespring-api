package org.corespring.platform.core.utils

object UnWordify {

  implicit class HTMLEntityConvert(string: String) {

    // Maps character codes to their HTML entity character equivalent
    val wordChars = Map(
      Seq(145, 8216) -> "&lsquo;",
      Seq(146, 8217) -> "&rsquo;",
      Seq(147, 8220) -> "&ldquo;",
      Seq(148, 8221) -> "&rdquo;",
      Seq(8211) -> "&ndash;",
      Seq(8212) -> "&mdash;"
    )

    /**
     * Removes Microsoft Word characters, converting them to HTML entity characters
     */
    def convertWordChars =
      wordChars.foldLeft(string)((str, replacement) =>
        replacement._1.foldLeft(str)((str, charCode) => str.replaceAll(charCode.toChar.toString, replacement._2)))
  }

}
