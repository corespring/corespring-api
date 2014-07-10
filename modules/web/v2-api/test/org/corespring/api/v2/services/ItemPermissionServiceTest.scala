package org.corespring.api.v2.services

import org.bson.types.ObjectId
import org.corespring.api.v2.errors.Errors._
import org.corespring.platform.core.models.auth.Permission
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.models.{ ContentCollRef, Organization }
import org.corespring.platform.core.services.organization.OrganizationService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.test.PlaySingleton
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import scalaz.{ Success, Failure }

class ItemPermissionServiceTest extends Specification with Mockito {

  PlaySingleton.start()

  val id = VersionedId(new ObjectId())
  val orgId = new ObjectId()
  val collectionId = new ObjectId().toString

  class scope(canAccess: Boolean = false) extends Scope {
    val s = new ItemPermissionService {
      override def organizationService: OrganizationService = {
        val m = mock[OrganizationService]
        m.canAccessCollection(any[ObjectId], any[ObjectId], any[Permission]) returns canAccess
        m
      }
    }
  }

  "ItemPermissionService" should {

    "when calling create" should {

      "fail - if no collection id" in new scope {
        s.create(Organization(), Item(id = id)) must_== Failure(noCollectionIdInItem(id))
      }

      "fail if missing content collection ref" in new scope {
        val item = Item(id = id, collectionId = Some(collectionId))
        s.create(Organization(id = orgId), item) must_== Failure(orgDoesntReferToCollection(orgId, collectionId))
      }

      "fail if read only " in new scope {
        val item = Item(id = id, collectionId = Some(collectionId))
        val organization = Organization(contentcolls = Seq(ContentCollRef(new ObjectId(collectionId), Permission.Read.value)))
        s.create(organization, item) must_== Failure(insufficientPermission(Permission.Read.value, Permission.Write))
      }

      "succeed" in new scope {
        val collectionId = ObjectId.get
        val item = Item(collectionId = Some(collectionId.toString))
        val organization = Organization(contentcolls = Seq(ContentCollRef(collectionId, Permission.Write.value)))
        s.create(organization, item) must_== Success(item)
      }

    }

    "when calling get" should {

      "fail if no collection id" in new scope {
        s.get(Organization(), Item(id = id)) must_== Failure(noCollectionIdInItem(id))
      }

      "fail if collection id is invalid" in new scope {
        val badCollectionId = "bad"
        val item = Item(id = id, collectionId = Some(badCollectionId))
        s.get(Organization(id = orgId), item) must_== Failure(invalidCollectionId(badCollectionId, item.id))
      }

      "fail if access denied" in new scope {
        val item = Item(id = id, collectionId = Some(collectionId))
        s.get(Organization(id = orgId), item) must_== Failure(inaccessibleItem(item.id, orgId, Permission.Read))
      }

      "succeed" in new scope(true) {
        val item = Item(id = id, collectionId = Some(collectionId))
        s.get(Organization(id = orgId), item) must_== Success(item)
      }
    }

  }
}
