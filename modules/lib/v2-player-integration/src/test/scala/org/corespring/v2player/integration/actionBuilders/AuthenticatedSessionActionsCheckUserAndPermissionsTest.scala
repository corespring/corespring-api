package org.corespring.v2player.integration.actionBuilders

import org.bson.types.ObjectId
import org.corespring.mongo.json.services.MongoService
import org.corespring.test.PlaySingleton
import org.corespring.test.matchers.RequestMatchers
import org.corespring.v2player.integration.actionBuilders.access.{ PlayerOptions, Mode => AccessMode }
import org.corespring.v2player.integration.errors.Errors._
import org.corespring.v2player.integration.errors.V2Error
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.JsValue
import play.api.mvc._
import play.api.test.{ FakeHeaders, FakeRequest }

import scalaz.{ Failure, Success, Validation }

class AuthenticatedSessionActionsCheckUserAndPermissionsTest
  extends Specification
  with Mockito
  with RequestMatchers {

  //TODO: There should be no need to have to init the play app when using a model
  //But at the moment the models are bound to a ModelCompanion object.
  PlaySingleton.start()

  import play.api.mvc.Results._
  import play.api.test.Helpers._

  def handler(r: Request[AnyContent]) = Ok("Worked")

  def fakeRequest = FakeRequest("", "", FakeHeaders(), AnyContentAsEmpty)

  def returnError(t: V2Error) = returnResult(t.code, t.message)

  "read" should {
    "when no user and permissions" should {
      s"return $UNAUTHORIZED " in new ActionsScope() {
        actions.read("id")(handler)(fakeRequest) must returnError(noUserOrOrgAccess)
      }

      s"return $OK" in new ActionsScope(true) {
        actions.read("id")(handler)(fakeRequest) must returnResult(200, "Worked")
      }
    }
  }

  class ActionsScope(
    hasAccess: Boolean = false,
    session: Option[JsValue] = None) extends Scope {

    def noUserOrOrgAccess = generalError(401, "No user permission or org access")

    def noUserPerms = generalError(401, "No user permission")

    def noOrgAccess = generalError(401, "No org access")

    val authCheck = {
      val o = mock[AuthCheck]
      o.hasAccess(
        any[RequestHeader],
        any[(ObjectId) => Validation[V2Error, Boolean]],
        any[(PlayerOptions) => Validation[String, Boolean]]) returns {
          if (hasAccess) Success(true) else Failure(noUserOrOrgAccess)
        }
      o
    }

    lazy val actions = {
      val sessionService = mock[MongoService]
      sessionService.load(anyString) returns session
      new AuthSessionActionsCheckPermissions(sessionService, authCheck) {}
    }
  }

}
