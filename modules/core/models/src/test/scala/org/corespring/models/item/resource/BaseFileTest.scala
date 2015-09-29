package org.corespring.models.item.resource

import org.specs2.matcher.{ Expectable, Matcher }
import org.specs2.mutable.Specification

class BaseFileTest extends Specification {

  import org.corespring.models.item.resource.BaseFile.ContentTypes._

  "BaseFile" should {

    case class matchContentType(expectedType: String) extends Matcher[String] {
      def apply[S <: String](s: Expectable[S]) = {

        val filename = s"some-file.${s.value}"

        val actualContentType = BaseFile.getContentType(filename)

        result(actualContentType == expectedType,
          s"${s.description} matches $expectedType",
          s"${s.description} does not match $expectedType",
          s)
      }
    }

    "files with no suffix have an unknown type" in BaseFile.getContentType("blah") === UNKNOWN
    "files with an unknown suffix have an unknown type" in BaseFile.getContentType("blah.blah") === UNKNOWN

    "accept all known file types" in {

      val suffixes = BaseFile.SuffixToContentTypes.toSeq

      val allSuffixes = suffixes ++ suffixes.map {
        tuple => (tuple._1.toUpperCase, tuple._2)
      }

      forall(allSuffixes) {
        (tuple: (String, String)) =>
          val (fileSuffix, contentType) = tuple
          fileSuffix must matchContentType(contentType)
      }
    }
  }
}
