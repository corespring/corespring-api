package org.corespring.v2.auth

import org.corespring.common.config.AppConfig
import org.corespring.v2.auth.models.{ Mode, PlayerAccessSettings }
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import play.api.Configuration

import scalaz.{ Failure, Success }

class AccessSettingsWildcardCheckTest extends Specification with Mockito {

  val config = mock[Configuration]
  config.getString(anyString, any) returns Some("test")
  config.getBoolean(anyString) returns Some(false)

  val appConfig = new AppConfig(config)
  val check = new AccessSettingsWildcardCheck(appConfig)

  val allow = check.allow _
  val notGrantedError = check.notGrantedError _

  "allow" should {

    "allow" in {
      allow("*", Some("*"), Mode.view, PlayerAccessSettings("*", Some("*"), false)) must_== Success(true)
    }

    "not allow if bad item id" in {
      check.allow("*", Some("*"), Mode.view, PlayerAccessSettings("1", Some("*"), false)) must_== Failure(notGrantedError("*", Some("*"), PlayerAccessSettings("1", Some("*"), false)))
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
