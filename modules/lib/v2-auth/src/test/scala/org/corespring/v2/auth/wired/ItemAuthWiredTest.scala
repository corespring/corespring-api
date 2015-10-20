package org.corespring.v2.auth.wired

import org.bson.types.ObjectId
import org.corespring.conversion.qti.transformers.ItemTransformer
import org.corespring.models.auth.Permission
import org.corespring.models.item.Item
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.item.ItemService
import org.corespring.v2.auth.ItemAccess
import org.corespring.v2.auth.models.{ AuthMode, MockFactory, OrgAndOpts, PlayerAccessSettings }
import org.corespring.v2.errors.Errors._
import org.corespring.v2.errors.V2Error
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import scalaz.{ Failure, Success, Validation }

class ItemAuthWiredTest extends Specification with Mockito with MockFactory {

  val defaultPermFailure = generalError("Perm failure")
  val defaultOrgAndOptsFailure = generalError("Org and opts failure")

  implicit val identity: OrgAndOpts = OrgAndOpts(mockOrg(), PlayerAccessSettings.ANYTHING, AuthMode.UserSession, None)

  def TestError(msg: String) = generalError(msg)

  case class authContext(
    item: Option[Item] = None,
    grantResult: Validation[V2Error, Boolean] = Failure(defaultPermFailure)) extends Scope {

    val mockItemService = {
      val m = mock[ItemService]
      m.findOneById(any[VersionedId[ObjectId]]) returns item
      m
    }

    lazy val itemTransformer: ItemTransformer = {
      val m = mock[ItemTransformer]
      m.updateV2Json(any[Item]).answers(i => i.asInstanceOf[Item])
      m
    }

    lazy val access: ItemAccess = {
      val m = mock[ItemAccess]
      m.grant(any[OrgAndOpts], any[Permission], any[Item]) returns grantResult
      m
    }

    lazy val itemService = mockItemService
    val itemAuth = new ItemAuthWired(mockItemService, itemTransformer, access)

  }

  "canRead" should {

    "fail if invalid item id" in new authContext() {
      itemAuth.loadForRead("?") must_== Failure(cantParseItemId("?"))
    }

    "fail if can't find item with id" in new authContext() {
      val vid = VersionedId(ObjectId.get, None)
      itemAuth.loadForRead(vid.toString) must_== Failure(cantFindItemWithId(vid))
    }

    "fail if there is a permission error" in new authContext(
      item = Some(Item(collectionId = ObjectId.get.toString))) {
      val vid = VersionedId(ObjectId.get, None)
      val identity = mockOrgAndOpts()
      itemAuth.loadForRead(vid.toString)(identity) must_== Failure(defaultPermFailure)
    }

    "succeed" in new authContext(
      item = Some(Item(collectionId = ObjectId.get.toString)),
      grantResult = Success(true)) {
      val vid = VersionedId(ObjectId.get, None)
      val identity = mockOrgAndOpts(AuthMode.UserSession)
      itemAuth.loadForRead(vid.toString)(identity) must_== Success(item.get)
    }
  }

  "delete" should {
    "call itemService.moveItemToArchive" in new authContext(
      item = Some(Item(collectionId = ObjectId.get.toString)),
      grantResult = Success(true)) {
      itemAuth.delete(item.get.id.toString)
      there was one(mockItemService).moveItemToArchive(item.get.id)
    }
  }

  "canWrite" should {
    "return true" in new authContext(
      item = Some(Item(collectionId = ObjectId.get.toString)),
      grantResult = Success(true)) {
      itemAuth.canWrite(item.get.id.toString) must_== Success(true)
    }

    "return false if grant fails" in new authContext(
      item = Some(Item(collectionId = ObjectId.get.toString)),
      grantResult = Failure(TestError("grant failed"))) {
      itemAuth.canWrite(item.get.id.toString) must_== Failure(TestError("grant failed"))
    }
  }
}
