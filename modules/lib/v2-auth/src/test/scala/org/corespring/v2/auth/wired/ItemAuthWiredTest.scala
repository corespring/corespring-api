package org.corespring.v2.auth.wired

import org.bson.types.ObjectId
import org.corespring.platform.core.models.Organization
import org.corespring.platform.core.models.auth.Permission
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.core.services.organization.OrganizationService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2.auth.models.{ AuthMode, OrgAndOpts, PlayerOptions }
import org.corespring.v2.errors.Errors._
import org.corespring.v2.errors.V2Error
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.mvc.RequestHeader

import scalaz.{ Failure, Success, Validation }

class ItemAuthWiredTest extends Specification with Mockito {

  val defaultPermFailure = generalError("Perm failure")
  val defaultOrgAndOptsFailure = generalError("Org and opts failure")

  implicit val identity: OrgAndOpts = OrgAndOpts(ObjectId.get, PlayerOptions.ANYTHING, AuthMode.UserSession)

  case class authContext(
    item: Option[Item] = None,
    org: Option[Organization] = None,
    perms: Validation[V2Error, Boolean] = Failure(defaultPermFailure),
    canAccess: Boolean = false) extends Scope {

    val itemAuth = new ItemAuthWired {
      override def itemService: ItemService = {
        val m = mock[ItemService]
        m.findOneById(any[VersionedId[ObjectId]]) returns item
        m
      }

      override def orgService: OrganizationService = {
        val m = mock[OrganizationService]
        m.findOneById(any[ObjectId]) returns org
        m.canAccessCollection(any[ObjectId], any[ObjectId], any[Permission]) returns canAccess
        m
      }

      override def hasPermissions(itemId: String, options: PlayerOptions): Validation[V2Error, Boolean] = {
        perms
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

      "fail if can't find org" in new authContext(
        item = Some(Item())) {
        val identity = OrgAndOpts(ObjectId.get, PlayerOptions.ANYTHING, AuthMode.UserSession)
        val vid = VersionedId(ObjectId.get, None)
        itemAuth.loadForRead(vid.toString)(identity) must_== Failure(cantFindOrgWithId(identity.orgId))
      }

      "fail if org can't access collection" in new authContext(
        item = Some(Item(collectionId = Some(ObjectId.get.toString))),
        org = Some(mock[Organization])) {
        val vid = VersionedId(ObjectId.get, None)
        val identity = OrgAndOpts(ObjectId.get, PlayerOptions.ANYTHING, AuthMode.UserSession)
        itemAuth.loadForRead(vid.toString)(identity) must_== Failure(orgCantAccessCollection(identity.orgId, item.get.collectionId.get, Permission.Read.name))
      }

      "fail if org can't access collection" in new authContext(
        item = Some(Item(collectionId = Some(ObjectId.get.toString))),
        org = Some(mock[Organization])) {
        val vid = VersionedId(ObjectId.get, None)
        val identity = OrgAndOpts(ObjectId.get, PlayerOptions.ANYTHING, AuthMode.UserSession)
        itemAuth.loadForRead(vid.toString)(identity) must_== Failure(orgCantAccessCollection(identity.orgId, item.get.collectionId.get, Permission.Read.name))
      }

      "fail if there is a permission error" in new authContext(
        item = Some(Item(collectionId = Some(ObjectId.get.toString))),
        org = Some(mock[Organization]),
        canAccess = true) {
        val vid = VersionedId(ObjectId.get, None)
        val identity = OrgAndOpts(ObjectId.get, PlayerOptions.ANYTHING, AuthMode.UserSession)
        itemAuth.loadForRead(vid.toString)(identity) must_== Failure(defaultPermFailure)
      }

      "succeed" in new authContext(
        item = Some(Item(collectionId = Some(ObjectId.get.toString))),
        org = Some(mock[Organization]),
        perms = Success(true),
        canAccess = true) {
        val vid = VersionedId(ObjectId.get, None)
        val identity = OrgAndOpts(ObjectId.get, PlayerOptions.ANYTHING, AuthMode.UserSession)
        itemAuth.loadForRead(vid.toString)(identity) must_== Success(item.get)
      }
    }

  }
}
