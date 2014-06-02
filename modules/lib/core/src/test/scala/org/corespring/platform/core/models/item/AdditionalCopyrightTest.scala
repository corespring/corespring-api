package org.corespring.platform.core.models.item

import org.corespring.platform.core.models.json.JsonValidationException
import play.api.libs.json._
import org.corespring.test.BaseTest

class AdditionalCopyrightTest extends BaseTest {

  "AdditionalCopyright" should {

    val jsonAdditionalCopyright = Json.obj("author" -> "author", "owner" -> "owner", "year" -> "year",
      "licenseType" -> "licenseType", "mediaType" -> "mediaType", "sourceUrl" -> "sourceUrl")

    val objAdditionalCopyright = AdditionalCopyright(Some("author"), Some("owner"), Some("year"), Some("licenseType"),
      Some("mediaType"), Some("sourceUrl"))

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
