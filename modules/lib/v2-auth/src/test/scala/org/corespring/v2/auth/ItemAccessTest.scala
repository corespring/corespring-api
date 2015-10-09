package org.corespring.v2.auth

import org.bson.types.ObjectId
import org.corespring.platform.core.models.Organization
import org.corespring.platform.core.models.auth.Permission
import org.corespring.platform.core.services.organization.OrganizationService
import org.corespring.test.PlaySingleton
import org.corespring.v2.auth.models.{ PlayerAccessSettings, AuthMode, MockFactory }
import org.corespring.v2.errors.Errors.{ invalidObjectId, generalError, orgCantAccessCollection, noCollectionIdForItem }
import org.corespring.v2.errors.V2Error
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import scalaz.{ Validation, Failure, Success }

class ItemAccessTest extends Specification with Mockito with MockFactory {

  PlaySingleton.start()

  class accessScope(orgCanAccess: Boolean = true,
    hasPermission: Boolean = true) extends Scope {

    lazy val opts = mockOrgAndOpts()

    val noPermission = generalError("no permission")

    lazy val access = new ItemAccess {

      override def hasPermissions(itemId: String, sessionId: Option[String], settings: PlayerAccessSettings): Validation[V2Error, Boolean] = {
        if (hasPermission) {
          Success(true)
        } else {
          Failure(noPermission)
        }
      }

      override def orgService: OrganizationService = {
        val m = mock[OrganizationService]
        m.canAccessCollection(
          any[Organization],
          any[ObjectId],
          any[Permission]) returns orgCanAccess
        m
      }
    }
  }

  "ItemAccess" should {

    "grant" should {
      "return Failure - if the item has no collection id" in new accessScope {
        val item = mockItem
        access.grant(opts, Permission.Write, item) must_==
          Failure(noCollectionIdForItem(item.id))
      }

      "return Failure - if org can't access" in new accessScope(false) {
        val item = mockItem.copy(collectionId = Some(ObjectId.get.toString))
        access.grant(opts, Permission.Write, item) must_==
          Failure(
            orgCantAccessCollection(opts.org.id,
              item.collectionId.getOrElse("?"), Permission.Write.name))
      }

      "return Failure -  permission is false" in new accessScope(hasPermission = false) {
        val item = mockItem.copy(collectionId = Some(ObjectId.get.toString))
        access.grant(opts, Permission.Write, item) must_== Failure(noPermission)
      }

      "return Success" in new accessScope {
        val item = mockItem.copy(collectionId = Some(ObjectId.get.toString))
        access.grant(opts, Permission.Write, item) must_==
          Success(true)
      }
    }

    "canCreateInCollection" should {

      class s(canAccess: Boolean = true) extends Scope {

        val opts = mockOrgAndOpts()

        lazy val access = new ItemAccess {
          override def orgService: OrganizationService = {
            val m = mock[OrganizationService]
            m.canAccessCollection(any[ObjectId], any[ObjectId], any[Permission]) returns canAccess
            m
          }
        }
      }

      "return Failure - if collection id is invalid" in new s {
        access.canCreateInCollection("?")(opts) must_== Failure(invalidObjectId("?", "collectionId"))
      }

      "return Failure - if org can't access" in new s(false) {
        val collectionId = ObjectId.get.toString
        access.canCreateInCollection(collectionId)(opts) must_== Failure(orgCantAccessCollection(opts.org.id, collectionId, Permission.Write.name))
      }

      "return Success" in new s {
        access.canCreateInCollection(ObjectId.get.toString)(opts) must_== Success(true)
      }
    }

  }
}
