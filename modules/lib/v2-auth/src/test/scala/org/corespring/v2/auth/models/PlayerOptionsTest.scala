package org.corespring.v2.auth.models

import org.specs2.mutable.Specification
import play.api.libs.json.Json

class PlayerOptionsTest extends Specification {

  "allowItemId" should {
    "allow itemId when starred" in PlayerAccessSettings("*", Some("*"), false).allowItemId("?") === true
    "not allow itemId when not starred" in PlayerAccessSettings("1", Some("*"), false).allowItemId("?") === false
    "allow itemId when not starred" in PlayerAccessSettings("1", Some("*"), false).allowItemId("1") === true
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
  }
}
