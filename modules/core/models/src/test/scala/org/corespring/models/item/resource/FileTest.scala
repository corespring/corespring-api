package org.corespring.models.item.resource

import org.bson.types.ObjectId
import org.corespring.platform.data.mongo.models.VersionedId
import org.specs2.matcher.{ Expectable, Matcher }
import org.specs2.mutable.Specification

class FileTest extends Specification {

  import org.corespring.models.item.resource.BaseFile.ContentTypes._

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

  "getContentType" should {
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

  "isValidContentType" should {

    "return true for all known types" in {

      forall(BaseFile.ContentTypes.binaryTypes ++ BaseFile.ContentTypes.textTypes) {
        t =>
          BaseFile.isValidContentType(t) === true
      }
    }

    "return false for other types" in {
      BaseFile.isValidContentType("blah") === false
    }
  }

  "StoredFile" should {
    "return a storage key for id with no version" in {
      val id = VersionedId(ObjectId.get)
      val resource = Resource(name = "resource", files = Seq())
      val file = StoredFile("img.png", "image/png")
      StoredFile.storageKey(id.id, 0, resource, file.name) === Seq(id.id, 0, resource.name, file.name).mkString("/")
    }

    "return a storage key for id with a version" in {
      val id = VersionedId(ObjectId.get, Some(0))
      val resource = Resource(name = "resource", files = Seq())
      val file = StoredFile("img.png", "image/png")
      StoredFile.storageKey(id.id, 0, resource, file.name) === Seq(id.id, 0, resource.name, file.name).mkString("/")
    }
  }

}