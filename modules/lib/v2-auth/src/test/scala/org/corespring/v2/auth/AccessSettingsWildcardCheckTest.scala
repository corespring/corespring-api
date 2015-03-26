package org.corespring.v2.auth

import org.corespring.v2.auth.models.{ Mode, PlayerAccessSettings }
import org.specs2.mutable.Specification

import scalaz.{ Failure, Success }

class AccessSettingsWildcardCheckTest extends Specification {

  import AccessSettingsWildcardCheck.notGrantedError
  val allow = AccessSettingsWildcardCheck.allow _

  "allow" should {

    "allow" in {
      allow("*", Some("*"), Mode.view, PlayerAccessSettings("*", Some("*"), false)) must_== Success(true)
    }

    "not allow if bad item id" in {
      allow("*", Some("*"), Mode.view, PlayerAccessSettings("1", Some("*"), false)) must_== Failure(notGrantedError("*", Some("*"), PlayerAccessSettings("1", Some("*"), false)))
    }

    "not allow if bad session id" in {
      allow("*", Some("*"), Mode.view, PlayerAccessSettings("*", Some("1"), false)) must_== Failure(notGrantedError("*", Some("*"), PlayerAccessSettings("*", Some("1"), false)))
    }

    "allow if only item id" in {
      allow("*", None, Mode.view, PlayerAccessSettings("*", Some("1"), false)) must_== Success(true)
    }

    "not allow if mode is wrong" in {
      allow("*", None, Mode.view, PlayerAccessSettings("*", Some("1"), false, mode = Some("gather"))) must_== Failure(notGrantedError("*", None, PlayerAccessSettings("*", Some("*"), false)))
    }.pendingUntilFixed("See:https://thesib.atlassian.net/browse/CA-1743")

    "allow if mode is wrong" in {
      allow("*", None, Mode.view, PlayerAccessSettings("*", Some("1"), false, mode = Some("*"))) must_== Success(true)
    }
  }

}
