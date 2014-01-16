package org.corespring.v2player.integration.actionBuilders

import org.bson.types.ObjectId
import org.corespring.mongo.json.services.MongoService
import org.corespring.platform.core.models.auth.Permission
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.models.{UserOrg, Organization, User}
import org.corespring.platform.core.services.UserService
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.core.services.organization.OrganizationService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.player.accessControl.cookies.PlayerCookieKeys._
import org.corespring.test.PlaySingleton
import org.corespring.test.matchers.RequestMatchers
import org.corespring.v2player.integration.actionBuilders.CheckUserAndPermissions.Errors
import org.corespring.v2player.integration.actionBuilders.access.{Mode, PlayerOptions}
import org.corespring.v2player.integration.securesocial.SecureSocialService
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.JsString
import play.api.libs.json.{Json, JsValue}
import play.api.mvc._
import play.api.test.FakeHeaders
import play.api.test.FakeRequest
import scala.Some
import scalaz.Success
import scalaz.Validation
import securesocial.core._

class AuthenticatedSessionActionsCheckUserAndPermissionsTest
  extends Specification 
  with Mockito 
  with RequestMatchers{

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

  def returnFromTuple(t:(Int,String)) = (returnResult _).tupled(t)

  "read" should {
    "when no user and permissions" should {
      s"return $UNAUTHORIZED " in new ActionsScope() {
        actions.read("id")(handler)(fakeRequest) must returnFromTuple(Errors.default)
      }
    }

    def withMaybeUserAndRequest(u:Option[User], id: Option[Identity], r : Request[AnyContent]) = {
      s"return $NOT_FOUND when session isn't found" in new ActionsScope(
        u, id
      ) {
        actions.read("id")(handler)(r) must returnFromTuple(Errors.cantLoadSession("id"))
      }

      s"return $BAD_REQUEST when the item id has a bad format" in new ActionsScope(
        u, id,  Some(Json.obj("itemId" -> JsString("bad string")))
      ) {
        actions.read("id")(handler)(r) must returnFromTuple(Errors.cantParseItemId)
      }

      s"return $NOT_FOUND when item id can't be found" in new ActionsScope(
        u, id, Some(Json.obj("itemId" -> JsString(itemId.toString)))
      ) {
        actions.read("id")(handler)(r) must returnFromTuple(Errors.cantFindItemWithId(itemId))
      }

      s"return $NOT_FOUND when org id can't be found" in new ActionsScope(
        u, id, Some(Json.obj("itemId" -> JsString(itemId.toString))), item(itemId)
      ) {
        actions.read("id")(handler)(r) must returnFromTuple(Errors.cantFindOrgWithId(orgId))
      }

      s"return $UNAUTHORIZED when org can't access item" in new ActionsScope(
        u, id, Some(Json.obj("itemId" -> JsString(itemId.toString))), item(itemId), org(orgId)
      ) {
        actions.read("id")(handler)(r) must returnFromTuple(Errors.default)
      }

      s"return $OK when org can access item" in new ActionsScope(
        u, id, Some(Json.obj("itemId" -> JsString(itemId.toString))), item(itemId), org(orgId), true, true
      ) {
        actions.read("id")(handler)(r) must returnFromTuple((OK,"Worked"))
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

        override def hasPermissions(itemId: String, sessionId: Option[String], mode: Mode.Mode, options: PlayerOptions): Validation[String, Boolean] = Success(userHasPermissions)
      }

    }
  }

}
