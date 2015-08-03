package org.corespring.v2.auth

import org.bson.types.ObjectId
import org.corespring.models.Organization
import org.corespring.models.auth.Permission
import org.corespring.services.OrganizationService
import org.corespring.v2.auth.models.Mode.Mode
import org.corespring.v2.auth.models.{ PlayerAccessSettings, MockFactory }
import org.corespring.v2.errors.Errors.{ invalidObjectId, generalError, orgCantAccessCollection, noCollectionIdForItem }
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import scalaz.{ Failure, Success }

class ItemAccessTest extends Specification with Mockito with MockFactory {

  class accessScope(orgCanAccess: Boolean = true,
    hasPermission: Boolean = true) extends Scope {

    lazy val opts = mockOrgAndOpts()

    val noPermission = generalError("no permission")

    lazy val orgService: OrganizationService = {
      val m = mock[OrganizationService]
      m.canAccessCollection(
        any[Organization],
        any[ObjectId],
        any[Permission]) returns orgCanAccess
      m
    }

    lazy val checker = {
      val m = mock[AccessSettingsWildcardCheck]
      m.allow(any[String], any[Option[String]], any[Mode], any[PlayerAccessSettings]) returns {
        if (hasPermission) {
          Success(true)
        } else {
          Failure(noPermission)
        }
      }

    }

    lazy val access = new ItemAccess(orgService, checker)
  }

  "ItemAccess" should {

    "grant" should {

      "return Failure - if org can't access" in new accessScope(false) {
        val item = mockItem.copy(collectionId = ObjectId.get.toString)
        access.grant(opts, Permission.Write, item) must_==
          Failure(
            orgCantAccessCollection(opts.org.id,
              item.collectionId, Permission.Write.name))
      }

      "return Failure - permission is false" in new accessScope(hasPermission = false) {
        val item = mockItem.copy(collectionId = ObjectId.get.toString)
        opts.copy(opts = opts.opts.copy())
        access.grant(opts, Permission.Write, item) must_== Failure(noPermission)
      }

      "return Success" in new accessScope {
        val item = mockItem.copy(collectionId = ObjectId.get.toString)
        access.grant(opts, Permission.Write, item) must_==
          Success(true)
      }
    }

    "canCreateInCollection" should {

      class s(canAccess: Boolean = true) extends Scope {

        val opts = mockOrgAndOpts()

        lazy val orgService = {
          val m = mock[OrganizationService]
          m.canAccessCollection(any[ObjectId], any[ObjectId], any[Permission]) returns canAccess
          m
        }

        lazy val checker = {
          val m = mock[AccessSettingsWildcardCheck]
          m.allow(any[String], any[Option[String]], any[Mode], any[PlayerAccessSettings]) returns Success(true)
        }

        lazy val access = new ItemAccess(orgService, checker)
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
