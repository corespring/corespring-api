package org.corespring.models.json.item

import org.corespring.models.item.{ AdditionalCopyright, FieldValue }
import org.specs2.mutable.Specification
import play.api.libs.json.{ Format, Json }

class AdditionalCopyrightFormatTest extends Specification {

  implicit val ac: Format[AdditionalCopyright] = new AdditionalCopyrightFormat {
    override def fieldValues: FieldValue = FieldValue()
  }

  "AdditionalCopyright" should {

    val jsonAdditionalCopyright = Json.obj("author" -> "author", "owner" -> "owner", "year" -> "year",
      "licenseType" -> "licenseType", "mediaType" -> "mediaType", "sourceUrl" -> "sourceUrl", "costForResource" -> 33)

    val objAdditionalCopyright = AdditionalCopyright(Some("author"), Some("owner"), Some("year"), Some("licenseType"),
      Some("mediaType"), Some("sourceUrl"), Some(33))

    "be serialized properly" in {
      val json = Json.toJson(objAdditionalCopyright)
      json must equalTo(jsonAdditionalCopyright)
    }

    "be serialized properly to a collection" in {
      val json = Json.toJson(List(objAdditionalCopyright))
      json must equalTo(Json.arr(jsonAdditionalCopyright))
    }

    "be deserialized properly" in {
      val obj = jsonAdditionalCopyright.as[AdditionalCopyright]
      obj must equalTo(objAdditionalCopyright)
    }

    "be deserialized properly to Seq" in {
      val seq = Json.arr(jsonAdditionalCopyright).as[Seq[AdditionalCopyright]]
      seq must equalTo(List(objAdditionalCopyright))
    }

  }

}
