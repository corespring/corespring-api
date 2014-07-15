package org.corespring.platform.core.models.item.resource

import org.specs2.execute.Result
import org.specs2.matcher.{ MatchResult, Expectable, Matcher }
import org.specs2.mutable.Specification
import play.api.libs.json.{ JsSuccess, Json }

class BaseFileTest extends Specification {

  import BaseFile.ContentTypes._

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

  "BaseFile reads" should {
    "read json for stored file with content incorrectly set" in {

      def matchType(fn: PartialFunction[BaseFile, Result])(t: (String, String)) = {
        val f = Json.obj("name" -> s"file.${t._1}", "content" -> "", "contentType" -> t._2)
        fn(f.as[BaseFile]).or(failure)
      }

      def findCt(t: String) = {
        BaseFile.SuffixToContentTypes.find { tuple =>
          tuple._2 == t
        }.getOrElse("unknown" -> BaseFile.ContentTypes.UNKNOWN)
      }

      val storedTypes = BaseFile.ContentTypes.binaryTypes.map(findCt)
      val textTypes = BaseFile.ContentTypes.textTypes.map(findCt)

      forall(storedTypes) {
        matchType {
          case StoredFile(_, _, _, _) => success
        }
      }

      forall(textTypes) {
        matchType {
          case VirtualFile(_, _, _, _) => success
        }
      }
    }
  }
}
