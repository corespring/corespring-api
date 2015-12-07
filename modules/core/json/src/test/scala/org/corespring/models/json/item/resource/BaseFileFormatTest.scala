package org.corespring.models.json.item.resource

import org.corespring.models.item.resource.{ BaseFile, StoredFile, VirtualFile }
import org.specs2.execute.Result
import org.specs2.mutable.Specification
import play.api.libs.json.{ Format, Json }

class BaseFileFormatTest extends Specification {

  implicit val format: Format[BaseFile] = BaseFileFormat

  "reads" should {

    "validates the contentType defined in the json and falls back to a known contentType if that validation fails" in {
      val json = Json.obj("name" -> "test.txt", "contentType" -> "blah", "isMain" -> false, "content" -> "hi")
      json.as[BaseFile] === VirtualFile("test.txt", "text/plain", false, "hi")
    }

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
