package org.corespring.v2player.integration.actionBuilders

import org.bson.types.ObjectId
import org.corespring.mongo.json.services.MongoService
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.models.{UserOrg, Organization, User}
import org.corespring.platform.core.services.UserService
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.core.services.organization.OrganizationService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.test.PlaySingleton
import org.corespring.v2player.integration.actionBuilders.CheckUserAndPermissions.Errors
import org.corespring.v2player.integration.actionBuilders.access.PlayerOptions
import org.corespring.v2player.integration.securesocial.SecureSocialService
import org.specs2.matcher.{Expectable, Matcher}
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.{JsString, Json, JsValue}
import play.api.mvc._
import play.api.test.FakeHeaders
import play.api.test.FakeRequest
import scalaz.Success
import scalaz.Validation
import securesocial.core._
import org.corespring.platform.core.models.auth.Permission
import play.api.test.FakeHeaders
import securesocial.core.IdentityId
import play.api.libs.json.JsString
import scala.Some
import scalaz.Success
import scala.concurrent.Future
import org.corespring.player.accessControl.cookies.PlayerCookieKeys._
import play.api.test.FakeHeaders
import securesocial.core.IdentityId
import play.api.libs.json.JsString
import scala.Some
import play.api.mvc.SimpleResult
import scalaz.Success
import play.api.mvc.Cookie

class AuthenticatedSessionActionsCheckUserAndPermissionsTest extends Specification with Mockito {

  //TODO: There should be no need to have to init the play app when using a model
  //But at the moment the models are bound to a ModelCompanion object.
  PlaySingleton.start()

  import play.api.mvc.Results._
  import play.api.test.Helpers._

  def handler(r: Request[AnyContent]) = Ok("Worked")

  def fakeRequest = FakeRequest("", "", FakeHeaders(), AnyContentAsEmpty)

  val orgId = ObjectId.get

  val itemId = VersionedId(ObjectId.get.toString).get
  val collectionId = ObjectId.get


  "read" should {
    "when no user and permissions" should {
      s"return $UNAUTHORIZED " in new ActionsScope() {
        actions.read("id")(handler)(fakeRequest) must returnCodeAndMessage(Errors.default)
      }
    }

    def withMaybeUserAndRequest(u:Option[User], id: Option[Identity], r : Request[AnyContent]) = {
      s"return $NOT_FOUND when session isn't found" in new ActionsScope(
        u, id
      ) {
        actions.read("id")(handler)(r) must returnCodeAndMessage(Errors.cantLoadSession("id"))
      }

      s"return $BAD_REQUEST when the item id has a bad format" in new ActionsScope(
        u, id,  Some(Json.obj("itemId" -> JsString("bad string")))
      ) {
        actions.read("id")(handler)(r) must returnCodeAndMessage(Errors.cantParseItemId)
      }

      s"return $NOT_FOUND when item id can't be found" in new ActionsScope(
        u, id, Some(Json.obj("itemId" -> JsString(itemId.toString)))
      ) {
        actions.read("id")(handler)(r) must returnCodeAndMessage(Errors.cantFindItemWithId(itemId))
      }

      s"return $NOT_FOUND when org id can't be found" in new ActionsScope(
        u, id, Some(Json.obj("itemId" -> JsString(itemId.toString))), item(itemId)
      ) {
        actions.read("id")(handler)(r) must returnCodeAndMessage(Errors.cantFindOrgWithId(orgId))
      }

      s"return $UNAUTHORIZED when org can't access item" in new ActionsScope(
        u, id, Some(Json.obj("itemId" -> JsString(itemId.toString))), item(itemId), org(orgId)
      ) {
        actions.read("id")(handler)(r) must returnCodeAndMessage(Errors.default)
      }

      s"return $OK when org can access item" in new ActionsScope(
        u, id, Some(Json.obj("itemId" -> JsString(itemId.toString))), item(itemId), org(orgId), true, true
      ) {
        actions.read("id")(handler)(r) must returnCodeAndMessage((OK,"Worked"))
      }
    }

    "when has user" should {
      withMaybeUserAndRequest(user("ed"), identity("ed"), fakeRequest)
    }

    "when anonymous user" should {

      val anonymousRequest = FakeRequest("","", FakeHeaders(), AnyContentAsEmpty)
        .withSession((ORG_ID, orgId.toString), (RENDER_OPTIONS, "{}"))
      withMaybeUserAndRequest(None, None, anonymousRequest)
    }
  }

  def item(itemId : VersionedId[ObjectId]) = {
    Some(Item(id = itemId, collectionId = collectionId.toString))
  }

  def org(orgId : ObjectId) = {
    Some(Organization(id = orgId))
  }

  def user(name: String) = Some(User(userName = "ed", org = UserOrg(orgId, 1)))

  def identity(name: String) = {
    val out = mock[Identity]
    out.identityId returns (new IdentityId(name, name))
    Some(out)
  }

  case class returnCodeAndMessage( expectedTuple : (Int,String)) extends Matcher[Future[SimpleResult]] {
    def apply[S <: Future[SimpleResult]](s: Expectable[S]) = {
      val (expectedStatus, expectedMsg) = expectedTuple
      val statusMatches = status(s.value) == expectedStatus
      val msgMatches = contentAsString(s.value) == expectedMsg
      val specsMsg = s"$expectedStatus =? ${status(s.value)} || $expectedMsg =? ${contentAsString(s.value)}"
      result(statusMatches && msgMatches, specsMsg, specsMsg, s)
    }
  }

  class ActionsScope(user: Option[User] = None,
                     identity: Option[Identity] = None,
                     session: Option[JsValue] = None,
                     item: Option[Item] = None,
                     org: Option[Organization] = None,
                     orgCanAccess : Boolean = false,
                     userHasPermissions : Boolean = false) extends Scope {

    lazy val actions = {
      val userService = mock[UserService]
      userService.getUser(anyString, anyString) returns user

      val secureSocialService = mock[SecureSocialService]
      secureSocialService.currentUser(any[Request[AnyContent]]) returns identity

      val sessionService = mock[MongoService]
      sessionService.load(anyString) returns session

      val itemService = mock[ItemService]
      itemService.findOneById(any[VersionedId[ObjectId]]) returns item

      val orgService = mock[OrganizationService]
      orgService.findOneById(any[ObjectId]) returns org
      orgService.canAccessCollection(any[ObjectId], any[ObjectId], any[Permission]) returns orgCanAccess

      new AuthenticatedSessionActionsCheckUserAndPermissions(
        secureSocialService,
        userService,
        sessionService,
        itemService,
        orgService
      ) {
        def hasPermissions(itemId: String, sessionId: Option[String], options: PlayerOptions): Validation[String, Boolean] = Success(userHasPermissions)
      }

    }
  }

}
