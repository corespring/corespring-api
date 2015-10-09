package org.corespring.v2.auth

import org.bson.types.ObjectId
import org.corespring.models.Organization
import org.corespring.models.appConfig.ArchiveConfig
import org.corespring.models.auth.Permission
import org.corespring.services.OrganizationService
import org.corespring.v2.auth.models.Mode.Mode
import org.corespring.v2.auth.models.{ PlayerAccessSettings, MockFactory }
import org.corespring.v2.auth.models.{ PlayerAccessSettings, AuthMode, MockFactory }
import org.corespring.v2.errors.Errors.{ invalidObjectId, generalError, orgCantAccessCollection, noCollectionIdForItem }
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import scalaz.{ Failure, Success }

class ItemAccessTest extends Specification with Mockito with MockFactory {

  trait base extends Scope {
    lazy val opts = mockOrgAndOpts()
    lazy val archiveConfig = ArchiveConfig(ObjectId.get, opts.org.id)

    val noPermission = generalError("no permission")

    lazy val orgService: OrganizationService = {
      val m = mock[OrganizationService]
      m
    }

    lazy val checker = {
      val m = mock[AccessSettingsWildcardCheck]
      m
    }

    lazy val access = new ItemAccess(orgService, checker, archiveConfig)

  }

  class accessScope(orgCanAccess: Boolean = true,
    hasPermission: Boolean = true) extends base {

    orgService.canAccessCollection(
      any[Organization],
      any[ObjectId],
      any[Permission]) returns orgCanAccess

    checker.allow(any[String], any[Option[String]], any[Mode], any[PlayerAccessSettings]) returns {
      if (hasPermission) {
        Success(true)
      } else {
        Failure(noPermission)
      }
    }
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

      class s(canAccess: Boolean = true) extends base {
        orgService.canAccessCollection(any[ObjectId], any[ObjectId], any[Permission]) returns canAccess
        checker.allow(any[String], any[Option[String]], any[Mode], any[PlayerAccessSettings]) returns Success(true)
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
