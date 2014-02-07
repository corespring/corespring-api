package org.corespring.play.json.salat.utils

import org.specs2.mutable.Specification
import com.mongodb.casbah.commons.MongoDBObject
import play.api.libs.json.Json

class ToJsValueTest extends Specification {

  "to js value" should {
    "stub test" in {
      val dbo = MongoDBObject("name" -> "ed")
      ToJsValue(dbo) === Json.obj("name" -> "ed")
    }
  }

}
