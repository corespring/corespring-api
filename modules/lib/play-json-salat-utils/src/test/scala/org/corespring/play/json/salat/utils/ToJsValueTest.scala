package org.corespring.play.json.salat.utils

import java.net.URL
import java.util.Date

import com.mongodb.casbah.commons.{MongoDBList, MongoDBObject}
import com.novus.salat.json.JSONConfig
import org.bson.types.ObjectId
import org.joda.time.DateTime
import org.specs2.mutable.Specification
import play.api.libs.json.{JsNull, Json}

class ToJsValueTest extends Specification {

  "to js value" should {
    "work with strings" in {
      val dbo = MongoDBObject("name" -> "ed")
      ToJsValue(dbo) === Json.obj("name" -> "ed")
    }
    "work with positive numbers" in {
      val dbo = MongoDBObject("name" -> 1)
      ToJsValue(dbo) === Json.obj("name" -> 1)
    }
    "work with negative numbers" in {
      val dbo = MongoDBObject("name" -> -1)
      ToJsValue(dbo) === Json.obj("name" -> -1)
    }
    "work with floats" in {
      val dbo = MongoDBObject("name" -> 1.4)
      ToJsValue(dbo) === Json.obj("name" -> 1.4)
    }
    "work with false" in {
      val dbo = MongoDBObject("name" -> false)
      ToJsValue(dbo) === Json.obj("name" -> false)
    }
    "work with true" in {
      val dbo = MongoDBObject("name" -> true)
      ToJsValue(dbo) === Json.obj("name" -> true)
    }
    "work with objects" in {
      val dbo = MongoDBObject("name" -> MongoDBObject("prop" -> "value"))
      ToJsValue(dbo) === Json.obj("name" -> Json.obj("prop" -> "value"))
    }
    "work with arrays" in {
      val dbo = MongoDBObject("name" -> MongoDBList("one", "two", "three"))
      ToJsValue(dbo) === Json.obj("name" -> Json.arr("one", "two", "three"))
    }
    "work with objectId" in {
      val oid = ObjectId.get
      val dbo = MongoDBObject("name" -> oid)
      ToJsValue(dbo) === Json.obj("name" -> oid.toString)
    }
    "work with urls" in {
      val value = new URL("http://corespring.org")
      val dbo = MongoDBObject("name" -> value)
      ToJsValue(dbo) === Json.obj("name" -> "http://corespring.org")
    }
    "work with urls" in {
      val value = new URL("http://corespring.org")
      val dbo = MongoDBObject("name" -> value)
      ToJsValue(dbo) === Json.obj("name" -> "http://corespring.org")
    }
    "work with Date" in {
      val dateFormatter = JSONConfig.ISO8601
      val value = new Date
      val dbo = MongoDBObject("name" -> value)
      ToJsValue(dbo) === Json.obj("name" -> dateFormatter.print(value.getTime))
    }
    "work with DateTime" in {
      val dateFormatter = JSONConfig.ISO8601
      val value = new DateTime
      val dbo = MongoDBObject("name" -> value)
      ToJsValue(dbo) === Json.obj("name" -> dateFormatter.print(value))
    }
    "work with null" in {
      val value = null
      val dbo = MongoDBObject("name" -> value)
      ToJsValue(dbo) === Json.obj("name" -> JsNull)
    }
  }

}
