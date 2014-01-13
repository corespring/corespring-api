package org.corespring.v2player.integration.actionBuilders.access

import org.specs2.mutable.Specification

class PlayerOptionsTest extends Specification {

  "allowItemId" should {
    "allow itemId when starred" in PlayerOptions("*","*",false).allowItemId("?") === true
    "not allow itemId when not starred" in PlayerOptions("1","*",false).allowItemId("?") === false
    "allow itemId when not starred" in PlayerOptions("1","*",false).allowItemId("1") === true
  }

  "allowSessionId" should {
    "allow sessionId when starred" in PlayerOptions("*","*",false).allowSessionId("?") === true
    "not allow sessionId when not starred" in PlayerOptions("*", "1",false).allowSessionId("?") === false
    "allow sessionId when not starred" in PlayerOptions("*","1",false).allowSessionId("1") === true
  }
}
