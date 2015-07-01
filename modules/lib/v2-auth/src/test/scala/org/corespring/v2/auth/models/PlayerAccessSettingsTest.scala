package org.corespring.v2.auth.models

import org.bson.types.ObjectId
import org.specs2.mutable.Specification
import play.api.libs.json.Json

class PlayerAccessSettingsTest extends Specification {

  val oid = ObjectId.get.toString
  "allowItemId" should {
    "allow itemId when starred" in PlayerAccessSettings("*", Some("*"), false).allowItemId("?") === true
    "not allow itemId when not starred" in PlayerAccessSettings(oid, Some("*"), false).allowItemId("?") === false
    "allow itemId when not starred" in PlayerAccessSettings(oid, Some("*"), false).allowItemId(oid) === true
    "allow any versioned itemId when settings.itemId has only the id specified" in {
      val settings = PlayerAccessSettings(oid, Some("*"), false)
      settings.allowItemId(s"$oid:0") === true
      settings.allowItemId(s"$oid:1") === true
      settings.allowItemId(s"$oid:100") === true
    }
    "only allow a versioned itemId when settings.itemId has a version" in {
      val settings = PlayerAccessSettings(s"$oid:0", Some("*"), false)
      settings.allowItemId(s"$oid:0") === true
      settings.allowItemId(s"$oid:1") === false
      settings.allowItemId(s"$oid:100") === false
    }
  }

  "allowSessionId" should {
    "allow sessionId when starred" in PlayerAccessSettings("*", Some("*"), false).allowSessionId("?") === true
    "not allow sessionId when not starred" in PlayerAccessSettings("*", Some("1"), false).allowSessionId("?") === false
    "allow sessionId when not starred" in PlayerAccessSettings("*", Some("1"), false).allowSessionId("1") === true
  }

  "json" should {
    "parse" in {
      val json = """{"mode":"*","itemId":"*","secure":false,"expires":0,"sessionId":"*"}"""
      Json.parse(json).asOpt[PlayerAccessSettings] === Some(PlayerAccessSettings("*", Some("*"), false, Some(0), Some("*")))
    }
    "parse expires is a number string" in {
      val json = """{"mode":"*","itemId":"*","secure":false,"expires":"0","sessionId":"*"}"""
      Json.parse(json).asOpt[PlayerAccessSettings] === Some(PlayerAccessSettings("*", Some("*"), false, Some(0), Some("*")))
    }

    "serialize expires as a string" in {
      val s = PlayerAccessSettings(itemId = "*", expires = Some(10))
      val json = Json.toJson(s)
      (json \ "expires").as[String] === "10"
    }
  }
}
