package org.corespring.v2player.integration.permissions

import org.corespring.v2.auth.models.{ Mode, PlayerOptions }
import org.corespring.v2.player.permissions.SimpleWildcardChecker
import org.specs2.mutable.Specification

class SimpleWildcardCheckerTest extends Specification {

  val checker = new SimpleWildcardChecker()

  import SimpleWildcardChecker._
  "allow" should {

    "allow" in {
      checker.allow("*", Some("*"), Mode.view, PlayerOptions("*", Some("*"), false)) === Right(true)
    }

    "not allow if bad item id" in {
      checker.allow("*", Some("*"), Mode.view, PlayerOptions("1", Some("*"), false)) === Left(notGrantedMsg)
    }

    "not allow if bad session id" in {
      checker.allow("*", Some("*"), Mode.view, PlayerOptions("*", Some("1"), false)) === Left(notGrantedMsg)
    }

    "allow if only item id" in {
      checker.allow("*", None, Mode.view, PlayerOptions("*", Some("1"), false)) === Right(true)
    }

    "not allow if mode is wrong" in {
      checker.allow("*", None, Mode.view, PlayerOptions("*", Some("1"), false, mode = Some("gather"))) === Left(notGrantedMsg)
    }.pendingUntilFixed("See:https://thesib.atlassian.net/browse/CA-1743")

    "allow if mode is wrong" in {
      checker.allow("*", None, Mode.view, PlayerOptions("*", Some("1"), false, mode = Some("*"))) === Right(true)
    }
  }

}
