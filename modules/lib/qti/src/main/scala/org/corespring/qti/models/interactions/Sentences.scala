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
  def split(string: String): Seq[String] = {
    sentenceDetector.sentDetect(string).toSeq
  }


}
