package org.corespring.v2player.integration.auth.wired

import org.bson.types.ObjectId
import org.corespring.platform.core.models.Organization
import org.corespring.platform.core.models.auth.Permission
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.core.services.organization.OrganizationService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2player.integration.cookies.PlayerOptions
import org.corespring.v2player.integration.cookies.Mode.Mode
import org.corespring.v2player.integration.errors.Errors._
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import scalaz.{ Success, Failure, Validation }

class ItemAuthWiredTest extends Specification with Mockito {

  val defaultPermFailure = "Perm failure"
  val defaultOrgAndOptsFailure = "Org and opts failure"

  implicit val rh: RequestHeader = FakeRequest("", "")

  case class authContext(item: Option[Item] = None,
    org: Option[Organization] = None,
    perms: Validation[String, Boolean] = Failure(defaultPermFailure),
    orgAndOpts: Validation[String, (ObjectId, PlayerOptions)] = Failure(defaultOrgAndOptsFailure),
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

      override def hasPermissions(itemId: String, sessionId: Option[String], mode: Mode, options: PlayerOptions): Validation[String, Boolean] = {
        perms
      }

      override def getOrgIdAndOptions(request: RequestHeader): Validation[String, (ObjectId, PlayerOptions)] = {
        orgAndOpts
      }
    }

  }

  "ItemAuthWired" should {

    "canRead" in {
      "fail if no orgId and opts" in new authContext() {
        itemAuth.loadForRead("") must_== Failure(noOrgIdAndOptions(rh).message)
      }

      "fail if invalid item id" in new authContext(orgAndOpts = Success(ObjectId.get -> PlayerOptions.ANYTHING)) {
        itemAuth.loadForRead("?") must_== Failure(cantParseItemId.message)
      }

      "fail if can't find item with id" in new authContext(orgAndOpts = Success(ObjectId.get -> PlayerOptions.ANYTHING)) {
        val vid = VersionedId(ObjectId.get, None)
        itemAuth.loadForRead(vid.toString) must_== Failure(cantFindItemWithId(vid).message)
      }

      "fail if can't find org" in new authContext(
        item = Some(Item()),
        orgAndOpts = Success(ObjectId.get -> PlayerOptions.ANYTHING)) {
        val vid = VersionedId(ObjectId.get, None)
        val orgId = orgAndOpts.map(_._1).toOption.get
        itemAuth.loadForRead(vid.toString) must_== Failure(cantFindOrgWithId(orgId).message)
      }

      "fail if org can't access collection" in new authContext(
        item = Some(Item(collectionId = Some(ObjectId.get.toString))),
        orgAndOpts = Success(ObjectId.get -> PlayerOptions.ANYTHING),
        org = Some(mock[Organization])) {
        val vid = VersionedId(ObjectId.get, None)
        val orgId = orgAndOpts.map(_._1).toOption.get
        itemAuth.loadForRead(vid.toString) must_== Failure(orgCantAccessCollection(orgId, item.get.collectionId.get).message)
      }

      "fail if org can't access collection" in new authContext(
        item = Some(Item(collectionId = Some(ObjectId.get.toString))),
        orgAndOpts = Success(ObjectId.get -> PlayerOptions.ANYTHING),
        org = Some(mock[Organization])) {
        val vid = VersionedId(ObjectId.get, None)
        val orgId = orgAndOpts.map(_._1).toOption.get
        itemAuth.loadForRead(vid.toString) must_== Failure(orgCantAccessCollection(orgId, item.get.collectionId.get).message)
      }

      "fail if there is a permission error" in new authContext(
        item = Some(Item(collectionId = Some(ObjectId.get.toString))),
        orgAndOpts = Success(ObjectId.get -> PlayerOptions.ANYTHING),
        org = Some(mock[Organization]),
        canAccess = true) {
        val vid = VersionedId(ObjectId.get, None)
        val orgId = orgAndOpts.map(_._1).toOption.get
        itemAuth.loadForRead(vid.toString) must_== Failure(defaultPermFailure)
      }

      "succeed" in new authContext(
        item = Some(Item(collectionId = Some(ObjectId.get.toString))),
        orgAndOpts = Success(ObjectId.get -> PlayerOptions.ANYTHING),
        org = Some(mock[Organization]),
        perms = Success(true),
        canAccess = true) {
        val vid = VersionedId(ObjectId.get, None)
        val orgId = orgAndOpts.map(_._1).toOption.get
        itemAuth.loadForRead(vid.toString) must_== Success(item.get)
      }
    }

  }
}
