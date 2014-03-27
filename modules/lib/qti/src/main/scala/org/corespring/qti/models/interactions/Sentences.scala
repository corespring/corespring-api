package org.corespring.qti.models.interactions

import opennlp.tools.sentdetect._
import java.io.FileInputStream
import scala.util.matching.Regex

/**
 * Utility for handling breaking down Strings into sentences. This implementation is currently based on Apache OpenNLP.
 */
object Sentences {

  val sentenceDetector = new SentenceDetectorME(
    new SentenceModel(new FileInputStream("modules/lib/qti/src/main/resources/en-sent.bin")))

  /**
   * Takes a String and returns a Seq[String] of the sentences in the String.
   */
  def split(string: String): Seq[String] = sentenceDetector.sentDetect(string).toSeq.moveTrailingToLeading

  implicit class StringSeqWithTrailingToLeading(strings: Seq[String]) {

    val TRAILING_CHARS = Seq('—')

    def regexForChar(char: Char): Regex = s"(.*[\\.\\?])($char.*)".r

    /**
     * For a Seq[String], any string ending with a sentence terminator followed by a blacklisted character and some
     * other characters has the characters after the sentence terminator moved to the head of the following String.
     * For example, if '—' is blacklisted:
     *
     *   Seq("this.", "is.—", "a.—Great", "test.")
     *
     * becomes
     *
     *   Seq("this.", "is.", "—a.", "—Great test.")
     *
     * This is done because Apache NLP sentence parsing has problems recognizing whether the '—' character at the end
     * of a sentence goes on the former or the latter.
     */
    def moveTrailingToLeading = {
      (List(List("", strings.head)) ++ strings.sliding(2)).map{ case(Seq(previous, current)) => {
        val currentWithoutTrailing = TRAILING_CHARS.find(c => { regexForChar(c).pattern.matcher(current).matches }) match {
          case Some(char) => {
            var re = regexForChar(char)
            current match {
              case re(prefix, suffix) => prefix
              case _ => current
            }
          }
          case _ => current
        }
        TRAILING_CHARS.find(c => regexForChar(c).pattern.matcher(previous).matches) match {
          case Some(char) => {
            val re = regexForChar(char)
            previous match {
              case re(prefix, suffix) => suffix.length match {
                case 1 => suffix + currentWithoutTrailing
                case _ => s"$suffix $currentWithoutTrailing"
              }
              case _ => currentWithoutTrailing
            }
          }
          case _ => currentWithoutTrailing
        }
      }}
    }
  }


}
