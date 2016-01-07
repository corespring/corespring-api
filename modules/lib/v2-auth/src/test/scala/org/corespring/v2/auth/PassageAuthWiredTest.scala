package org.corespring.v2.auth

import org.bson.types.ObjectId
import org.corespring.models.Organization
import org.corespring.models.auth.Permission
import org.corespring.models.item.Passage
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.PassageService
import org.corespring.v2.auth.models.AuthMode.AuthMode
import org.corespring.v2.auth.models.{PlayerAccessSettings, OrgAndOpts}
import org.corespring.v2.errors.Errors.{invalidObjectId, cantFindPassageWithId, inaccessiblePassage}
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import scala.concurrent.Future
import scalaz.{Failure, Success}

class PassageAuthWiredTest extends Specification with Mockito {

  trait PassageAuthScope extends Scope {
    val passageService = mock[PassageService]
    val access = mock[PassageAccess]
    val passageAuthWired = new PassageAuthWired(passageService, access)

    val passageId = new VersionedId[ObjectId](new ObjectId(), Some(0))
    val orgId = new ObjectId()
    val org = Organization(name = "", id = orgId)
    val identity = OrgAndOpts(org = org, opts = mock[PlayerAccessSettings], authMode = mock[AuthMode],
      apiClientId = None)
    val passage = mock[Passage]
  }

  "loadForRead" should {

    "with invalid object id" should {

      trait InvalidIdPassageAuthScope extends PassageAuthScope {
        val invalidId = "Not a versioned identifier"
      }

      "return invalidObjectId error" in new InvalidIdPassageAuthScope {
        passageAuthWired.loadForRead(invalidId)(identity) must be equalTo(Failure(invalidObjectId(invalidId, "")))
      }

    }

    "passage does not exist" should {

      trait PassageDoesntExistScope extends PassageAuthScope {
        passageService.get(passageId) returns Future.successful(Success(None))
      }

      "returns cantFindPassageWithId error" in new PassageDoesntExistScope {
        passageAuthWired.loadForRead(passageId.toString)(identity) must be equalTo
          (Failure(cantFindPassageWithId(passageId)))
      }

    }

    "passage exists" should {

      trait PassageExistsScope extends PassageAuthScope {
        passageService.get(passageId) returns Future.successful(Success(Some(passage)))
      }

      "access not granted" should {
        trait PassagePresentAccessNotGrantedScope extends PassageExistsScope {
          access.grant(identity, Permission.Read, (passage, None)) returns Success(false)
        }

        "return inaccessiblePassage error" in new PassagePresentAccessNotGrantedScope {
          passageAuthWired.loadForRead(passageId.toString)(identity) must be equalTo
            (Failure(inaccessiblePassage(passageId, orgId, Permission.Read)))
        }
      }

      "access granted" should {

        trait PassagePresentAccessGrantedScope extends PassageExistsScope {
          access.grant(identity, Permission.Read, (passage, None)) returns Success(true)
        }

        "return passage" in new PassagePresentAccessGrantedScope {
          passageAuthWired.loadForRead(passageId.toString)(identity) must be equalTo Success(passage)
        }

      }

    }

  }

}
