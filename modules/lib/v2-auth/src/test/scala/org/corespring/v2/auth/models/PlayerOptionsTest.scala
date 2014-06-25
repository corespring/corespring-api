package org.corespring.v2.auth.models

import org.specs2.mutable.Specification

class PlayerOptionsTest extends Specification {

  "allowItemId" should {
    "allow itemId when starred" in PlayerOptions("*", Some("*"), false).allowItemId("?") === true
    "not allow itemId when not starred" in PlayerOptions("1", Some("*"), false).allowItemId("?") === false
    "allow itemId when not starred" in PlayerOptions("1", Some("*"), false).allowItemId("1") === true
  }

  "allowSessionId" should {
    "allow sessionId when starred" in PlayerOptions("*", Some("*"), false).allowSessionId("?") === true
    "not allow sessionId when not starred" in PlayerOptions("*", Some("1"), false).allowSessionId("?") === false
    "allow sessionId when not starred" in PlayerOptions("*", Some("1"), false).allowSessionId("1") === true
  }
}
