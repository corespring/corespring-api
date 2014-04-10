package org.corespring.v2player.integration.controllers.editor

import org.bson.types.ObjectId
import org.corespring.platform.core.models.auth.Permission
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.platform.core.services.UserService
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.core.services.organization.OrganizationService
import org.corespring.test.matchers.RequestMatchers
import org.corespring.v2player.integration.actionBuilders.access.PlayerOptions
import org.corespring.v2player.integration.errors.{ Errors, V2Error }
import org.corespring.v2player.integration.securesocial.SecureSocialService
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.{ Json, JsValue }
import play.api.mvc.Results._
import play.api.mvc.{ RequestHeader, SimpleResult }
import play.api.test.FakeRequest
import play.api.test.Helpers._
import scala.concurrent.{ Future, ExecutionContext }

class ItemHooksTest extends Specification with Mockito with RequestMatchers {

  import ExecutionContext.Implicits.global
  import scala.language.higherKinds

  abstract class baseContext[ERR, RES](val itemId: String = ObjectId.get.toString,
    val item: Option[Item] = None,
    val orgIdAndOptions: Option[(ObjectId, PlayerOptions)] = None,
    val canAccessCollection: Boolean = false) extends Scope {

    lazy val vid = VersionedId(new ObjectId(itemId))

    lazy val header = FakeRequest("", "")

    def f: Future[Either[ERR, RES]]

    def toSimpleResult(f: Future[Either[ERR, RES]]): Future[SimpleResult]

    lazy val result: Future[SimpleResult] = toSimpleResult(f)

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

        m.insert(any[Item]) returns Some(VersionedId(ObjectId.get))
        m
      }

      override def getOrgIdAndOptions(header: RequestHeader) = {
        orgIdAndOptions
      }
    }
  }

  class loadContext(
    itemId: String = ObjectId.get.toString,
    item: Option[Item] = None,
    orgIdAndOptions: Option[(ObjectId, PlayerOptions)] = None,
    canAccessCollection: Boolean = false)
    extends baseContext[SimpleResult, JsValue](itemId, item, orgIdAndOptions, canAccessCollection) {
    lazy val f = hooks.load(itemId)(FakeRequest("", ""))

    override def toSimpleResult(f: Future[Either[SimpleResult, JsValue]]): Future[SimpleResult] = f.map {
      case Left(r) => r
      case Right(json) => Ok(json)
    }

  }

  class saveContext(itemId: String = ObjectId.get.toString,
    item: Option[Item] = None,
    orgIdAndOptions: Option[(ObjectId, PlayerOptions)] = None,
    canAccessCollection: Boolean = false,
    val json: JsValue = Json.obj()) extends baseContext[SimpleResult, JsValue](itemId, item, orgIdAndOptions, canAccessCollection) {

    lazy val f = hooks.save(itemId, json)(header)

    override def toSimpleResult(f: Future[Either[SimpleResult, JsValue]]): Future[SimpleResult] = f.map {
      either =>
        either match {
          case Left(r) => r
          case Right(json) => Ok(json)
        }
    }
  }

  def returnError(e: V2Error) = returnResult(e.code, e.message)

  class createContext(
    val json: Option[JsValue] = None,
    orgIdAndOptions: Option[(ObjectId, PlayerOptions)] = None,
    canAccessCollection: Boolean = false)
    extends baseContext[(Int, String), String](
      orgIdAndOptions = orgIdAndOptions,
      canAccessCollection = canAccessCollection) {
    override def toSimpleResult(f: Future[Either[(Int, String), String]]): Future[SimpleResult] = f.map {
      case Left((code, message)) => Status(code)(message)
      case Right(msg) => Ok(msg)
    }

    override def f: Future[Either[(Int, String), String]] = hooks.create(json)(header)
  }

  "load" should {

    "return can't find item id error" in new loadContext() {
      result must returnError(Errors.cantFindItemWithId(vid))
    }

    "return bad request for bad item id" in new loadContext("") {
      result must returnError(Errors.cantParseItemId)
    }

    "return org can't access item error" in new loadContext(item = Some(Item())) {
      val e = Errors.noOrgIdAndOptions(header)
      result must returnError(e)
    }

    "return an item" in new loadContext(
      item = Some(Item(collectionId = Some(ObjectId.get.toString))),
      orgIdAndOptions = Some(ObjectId.get, PlayerOptions.ANYTHING),
      canAccessCollection = true) {
      status(result) === OK
    }
  }

  "save" should {

    "return not found" in new saveContext() {
      result must returnError(Errors.cantFindItemWithId(vid))
    }

    "return cantParse error for a bad item id" in new saveContext("bad id") {
      result must returnError(Errors.cantParseItemId)
    }

    "return no OrgId and Options error" in new saveContext(item = Some(Item())) {
      result must returnError(Errors.noOrgIdAndOptions(header))
    }

    "return no collection id error" in new saveContext(
      item = Some(Item()),
      orgIdAndOptions = Some(ObjectId.get -> PlayerOptions.ANYTHING)) {
      result must returnError(Errors.noCollectionIdForItem(vid))
    }

    "return org can't access collection error" in new saveContext(
      item = Some(Item(collectionId = Some(ObjectId.get.toString))),
      orgIdAndOptions = Some(ObjectId.get -> PlayerOptions.ANYTHING)) {
      result must returnError(Errors.orgCantAccessCollection(orgIdAndOptions.get._1, item.get.collectionId.get))
    }

    "save update" in new saveContext(
      item = Some(Item(collectionId = Some(ObjectId.get.toString))),
      orgIdAndOptions = Some(ObjectId.get -> PlayerOptions.ANYTHING),
      canAccessCollection = true,
      json = Json.obj(
        "profile" -> Json.obj(),
        "components" -> Json.obj(),
        "xhtml" -> "<div/>")) {

      status(result) === OK
      contentAsJson(result) === json
    }
  }

  "create" should {

    "return no json error" in new createContext(None) {
      result must returnError(Errors.noJson)
    }

    "return property not found" in new createContext(Some(Json.obj())) {
      result must returnError(Errors.propertyNotFoundInJson("collectionId"))
    }

    "return no org id and options" in new createContext(Some(Json.obj("collectionId" -> ObjectId.get.toString))) {
      result must returnError(Errors.noOrgIdAndOptions(header))
    }

    "return no org id and options" in new createContext(
      Some(Json.obj("collectionId" -> ObjectId.get.toString)),
      Some((ObjectId.get, PlayerOptions.ANYTHING))) {
      result must returnError(Errors.orgCantAccessCollection(orgIdAndOptions.get._1, (json.get \ "collectionId").as[String]))
    }

    "return item id for new item" in new createContext(
      Some(Json.obj("collectionId" -> ObjectId.get.toString)),
      Some((ObjectId.get, PlayerOptions.ANYTHING)),
      true) {
      status(result) === OK
    }
  }
}
