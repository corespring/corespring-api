package org.corespring.v2.auth.wired

import org.bson.types.ObjectId
import org.corespring.platform.core.models.{ ContentCollection, Organization }
import org.corespring.platform.core.models.auth.Permission
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.BaseFindAndSaveService
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.core.services.organization.OrganizationService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2.auth.ItemAccess
import org.corespring.v2.auth.models.{ MockFactory, AuthMode, OrgAndOpts, PlayerAccessSettings }
import org.corespring.qtiToV2.transformers.ItemTransformer
import org.corespring.v2.auth.models.{ AuthMode, OrgAndOpts, PlayerAccessSettings }
import org.corespring.v2.errors.Errors._
import org.corespring.v2.errors.V2Error
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.Configuration
import play.api.mvc.RequestHeader

import scalaz.{ Failure, Success, Validation }

class ItemAuthWiredTest extends Specification with Mockito with MockFactory {

  val defaultPermFailure = generalError("Perm failure")
  val defaultOrgAndOptsFailure = generalError("Org and opts failure")

  implicit val identity: OrgAndOpts = OrgAndOpts(mockOrg(), PlayerAccessSettings.ANYTHING, AuthMode.UserSession, None)

  case class authContext(
    item: Option[Item] = None,
    org: Option[Organization] = None,
    grantResult: Validation[V2Error, Boolean] = Failure(defaultPermFailure)) extends Scope {

    val itemAuth = new ItemAuthWired {

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

      lazy val itemService = {
        val m = mock[ItemService]
        m.findOneById(any[VersionedId[ObjectId]]) returns item
        m
      }
    }
  }

  "ItemAuthWired" should {

    "canRead" in {

      "fail if invalid item id" in new authContext() {
        itemAuth.loadForRead("?") must_== Failure(cantParseItemId("?"))
      }

      "fail if can't find item with id" in new authContext() {
        val vid = VersionedId(ObjectId.get, None)
        itemAuth.loadForRead(vid.toString) must_== Failure(cantFindItemWithId(vid))
      }

      "fail if there is a permission error" in new authContext(
        item = Some(Item(collectionId = Some(ObjectId.get.toString))),
        org = Some(mock[Organization])) {
        val vid = VersionedId(ObjectId.get, None)
        val identity = mockOrgAndOpts()
        itemAuth.loadForRead(vid.toString)(identity) must_== Failure(defaultPermFailure)
      }

      "succeed" in new authContext(
        item = Some(Item(collectionId = Some(ObjectId.get.toString))),
        org = Some(mock[Organization]),
        grantResult = Success(true)) {
        val vid = VersionedId(ObjectId.get, None)
        val identity = mockOrgAndOpts(AuthMode.UserSession)
        itemAuth.loadForRead(vid.toString)(identity) must_== Success(item.get)
      }
    }

  }
}
