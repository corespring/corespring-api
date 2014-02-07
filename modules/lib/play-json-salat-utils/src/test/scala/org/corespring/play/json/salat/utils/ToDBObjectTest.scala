package org.corespring.play.json.salat.utils

import com.mongodb.casbah.commons.MongoDBObject
import org.specs2.mutable.Specification
import play.api.libs.json.Json

class ToDBObjectTest extends Specification {

  "to dbo" should {
    "stub test" in {
      val json = Json.obj("name" -> "ed")
      ToDBObject(json) === MongoDBObject("name" -> "ed")
    }
  }

}
