package org.corespring.v2player.integration.controllers.editor

import java.util.concurrent.TimeUnit
import org.bson.types.ObjectId
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.UserService
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.core.services.organization.OrganizationService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2player.integration.securesocial.SecureSocialService
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.{ Json, JsValue }
import play.api.mvc.{ RequestHeader, SimpleResult }
import play.api.test.FakeRequest
import scala.concurrent.duration.Duration
import scala.concurrent.{ Future, Await, ExecutionContext }
import play.api.test.Helpers._
import play.api.mvc.Results._
import org.corespring.v2player.integration.errors.Errors
import org.corespring.test.matchers.RequestMatchers
import org.corespring.v2player.integration.actionBuilders.access.PlayerOptions
import org.corespring.platform.core.models.auth.Permission

class ItemHooksTest extends Specification with Mockito with RequestMatchers {

  import ExecutionContext.Implicits.global

  import scala.language.higherKinds

  abstract class baseContext[ERR, RES](val itemId: String = ObjectId.get.toString,
    val item: Option[Item] = None,
    val orgIdAndOptions: Option[(ObjectId, PlayerOptions)] = None,
    val canAccessCollection: Boolean = false) extends Scope {
    lazy val header = FakeRequest("", "")

    def f: Future[Either[ERR, RES]]

    def toSimpleResult(f: Future[Either[ERR, RES]]): Future[SimpleResult]

    lazy val futureResult: Future[SimpleResult] = toSimpleResult(f)

    lazy val hooks = new ItemHooks {
      override def orgService: OrganizationService = {
        val m = mock[OrganizationService]
        m.canAccessCollection(any[ObjectId], any[ObjectId], any[Permission]) returns canAccessCollection
        m
      }

      override def userService: UserService = mock[UserService]

      override def secureSocialService: SecureSocialService = mock[SecureSocialService]

      override implicit def executionContext: ExecutionContext = ExecutionContext.Implicits.global

      override def transform: (Item) => JsValue = (i: Item) => Json.obj()

      override def itemService: ItemService = {
        val m = mock[ItemService]
        m.findOneById(any[VersionedId[ObjectId]]) returns item
        m
      }

      override def getOrgIdAndOptions(header: RequestHeader) = {
        orgIdAndOptions
      }
    }
  }

  class loadContext(itemId: String = ObjectId.get.toString, item: Option[Item] = None) extends baseContext[SimpleResult, JsValue](itemId, item, None, false) {
    lazy val f = hooks.load(itemId)(FakeRequest("", ""))

    override def toSimpleResult(f: Future[Either[SimpleResult, JsValue]]): Future[SimpleResult] = f.map {
      case Left(r) => r
      case Right(json) => Ok(json)
    }
  }

  class saveContext(itemId: String = ObjectId.get.toString,
    item: Option[Item] = None,
    orgIdAndOptions: Option[(ObjectId, PlayerOptions)] = None,
    canAccessCollection: Boolean = false) extends baseContext[SimpleResult, JsValue](itemId, item, orgIdAndOptions, canAccessCollection) {

    lazy val f = hooks.save(itemId, Json.obj())(header)

    override def toSimpleResult(f: Future[Either[SimpleResult, JsValue]]): Future[SimpleResult] = f.map {
      either =>
        either match {
          case Left(r) => r
          case Right(json) => Ok(json)
        }
    }
  }

  "load" should {

    "return not found for no item" in new loadContext() {
      status(futureResult) === NOT_FOUND
    }

    "return not found for bad item id" in new loadContext("") {
      status(futureResult) === NOT_FOUND

    }

    "return ok for item" in new loadContext(item = Some(Item())) {
      status(futureResult) === OK
    }
  }

  "save" should {

    "return not found" in new saveContext() {
      val e = Errors.cantFindItemWithId(VersionedId(new ObjectId(itemId)))
      futureResult must returnResult(e.code, e.message)
    }

    "return cantParse error for a bad item id" in new saveContext("bad id") {
      val e = Errors.cantParseItemId
      futureResult must returnResult(e.code, e.message)
    }

    "return no OrgId and Options error" in new saveContext(item = Some(Item())) {
      val e = Errors.noOrgIdAndOptions(header)
      futureResult must returnResult(e.code, e.message)
    }

    "return no collection id error" in new saveContext(
      item = Some(Item()),
      orgIdAndOptions = Some(ObjectId.get -> PlayerOptions.ANYTHING)) {
      val e = Errors.noCollectionIdForItem(VersionedId(new ObjectId(itemId)))
      futureResult must returnResult(e.code, e.message)
    }

    "return " in new saveContext(
      item = Some(Item(collectionId = Some(ObjectId.get.toString))),
      orgIdAndOptions = Some(ObjectId.get -> PlayerOptions.ANYTHING)) {
      val e = Errors.orgCantAccessCollection(orgIdAndOptions.get._1, item.get.collectionId.get)
      futureResult must returnResult(e.code, e.message)
    }
  }
}
