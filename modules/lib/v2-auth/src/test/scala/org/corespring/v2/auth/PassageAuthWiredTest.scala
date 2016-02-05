package org.corespring.v2.auth

import org.bson.types.ObjectId
import org.corespring.errors.{PassageSaveError, PassageInsertError}
import org.corespring.models.Organization
import org.corespring.models.auth.Permission
import org.corespring.models.item.Passage
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.{OrgCollectionService, PassageService}
import org.corespring.v2.auth.models.AuthMode.AuthMode
import org.corespring.v2.auth.models.{PlayerAccessSettings, OrgAndOpts}
import org.corespring.v2.errors.Errors._
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scalaz.{Failure, Success}

class PassageAuthWiredTest extends Specification with Mockito {

  trait PassageAuthScope extends Scope {
    val executionContext = ExecutionContext.global
    val passageService = mock[PassageService]
    val access = mock[PassageAccess]
    val passageAuthWired = new PassageAuthWired(passageService, access)

    val passageId = new VersionedId[ObjectId](new ObjectId(), Some(0))
    val collectionId = new ObjectId()
    val orgId = new ObjectId()
    val org = Organization(name = "", id = orgId)
    val identity = OrgAndOpts(org = org, opts = mock[PlayerAccessSettings], authMode = mock[AuthMode],
      apiClientId = None)
    val passage = Passage(id = passageId, collectionId = collectionId.toString)
    val ec = ExecutionContext.global
  }

  "loadForRead" should {

    "with invalid object id" should {

      trait InvalidIdPassageAuthScope extends PassageAuthScope {
        val invalidId = "Not a versioned identifier"
      }

      "return invalidObjectId error" in new InvalidIdPassageAuthScope {
        Await.result(passageAuthWired.loadForRead(invalidId)(identity, executionContext), Duration.Inf) must be equalTo(
          Failure(invalidObjectId(invalidId, "")))
      }

    }

    "passage does not exist" should {

      trait PassageDoesntExistScope extends PassageAuthScope {
        passageService.get(passageId) returns Future.successful(Success(None))
      }

      "returns cantFindPassageWithId error" in new PassageDoesntExistScope {
        Await.result(passageAuthWired.loadForRead(passageId.toString)(identity, executionContext), Duration.Inf) must be equalTo
          (Failure(cantFindPassageWithId(passageId)))
      }

    }

    "passage exists" should {

      trait PassageExistsScope extends PassageAuthScope {
        passageService.get(passageId) returns Future.successful(Success(Some(passage)))
      }

      "access not granted" should {
        trait PassagePresentAccessNotGrantedScope extends PassageExistsScope {
          access.grant(identity, Permission.Read, (passage, None)) returns Future.successful(Success(false))
        }

        "return inaccessiblePassage error" in new PassagePresentAccessNotGrantedScope {
          Await.result(passageAuthWired.loadForRead(passageId.toString)(identity, executionContext), Duration.Inf) must be equalTo
            (Failure(inaccessiblePassage(passageId, orgId, Permission.Read)))
        }
      }

      "access granted" should {

        trait PassagePresentAccessGrantedScope extends PassageExistsScope {
          access.grant(identity, Permission.Read, (passage, None)) returns Future.successful(Success(true))
        }

        "return passage" in new PassagePresentAccessGrantedScope {
          Await.result(passageAuthWired.loadForRead(passageId.toString)(identity, executionContext), Duration.Inf) must be equalTo Success(passage)
        }

      }

    }

  }

  "insert" should {

    "cannot write with provided identity" should {

      trait CannotWriteScope extends PassageAuthScope {
        access.grant(identity, Permission.Write, (passage, None)) returns Future.successful(Success(false))
      }

      "return couldNotWritePassage error" in new CannotWriteScope {
        val result = Await.result(passageAuthWired.insert(passage)(identity, ec), Duration.Inf)
        result must be equalTo(Failure(couldNotWritePassage(passage.id)))
      }

    }

    "can write with provided identity" should {

      trait CanWriteScope extends PassageAuthScope {
        access.grant(identity, Permission.Write, (passage, None)) returns Future.successful(Success(true))
      }

      "passageService#insert fails" should {

        trait CanWriteServiceFailsScope extends CanWriteScope {
          val error = PassageInsertError()
          passageService.insert(any[Passage]) returns Future.successful(Failure(error))
        }

        "return couldNotSavePassage error" in new CanWriteServiceFailsScope {
          Await.result(passageAuthWired.insert(passage)(identity, ec), Duration.Inf) must be equalTo
            (Await.result(Future.successful(Failure(couldNotCreatePassage())), Duration.Inf))
        }

      }

      "passageService#save succeeds" should {

        trait CanWriteServiceSucceedsScope extends CanWriteScope {
          passageService.insert(any[Passage]) returns Future.successful(Success(passage))
        }

        "return result from passageService#insert" in new CanWriteServiceSucceedsScope {
          Await.result(passageAuthWired.insert(passage)(identity, ec), Duration.Inf) must be equalTo
            (Await.result(Future.successful(Success(passage)), Duration.Inf))
        }

      }


    }


  }


  "save" should {

    "cannot write with provided identity" should {

      trait CannotWriteScope extends PassageAuthScope {
        access.grant(identity, Permission.Write, (passage, None)) returns Future.successful(Success(false))
      }

      "return couldNotWritePassage error" in new CannotWriteScope {
        val result = Await.result(passageAuthWired.save(passage)(identity, ec), Duration.Inf)
        result must be equalTo(Failure(couldNotWritePassage(passage.id)))
      }

    }

    "can write with provided identity" should {

      trait CanWriteScope extends PassageAuthScope {
        access.grant(identity, Permission.Write, (passage, None)) returns Future.successful(Success(true))
      }

      "passageService#save fails" should {

        trait CanWriteServiceFailsScope extends CanWriteScope {
          val error = PassageSaveError(passageId)
          passageService.save(any[Passage]) returns Future.successful(Failure(error))
        }

        "return couldNotSavePassage error" in new CanWriteServiceFailsScope {
          Await.result(passageAuthWired.save(passage)(identity, ec), Duration.Inf) must be equalTo
            (Await.result(Future.successful(Failure(couldNotSavePassage(passageId))), Duration.Inf))
        }

      }

      "passageService#save succeeds" should {

        trait CanWriteServiceSucceedsScope extends CanWriteScope {
          passageService.save(any[Passage]) returns Future.successful(Success(passage))
        }

        "return result from passageService#save" in new CanWriteServiceSucceedsScope {
          Await.result(passageAuthWired.save(passage)(identity, ec), Duration.Inf) must be equalTo
            (Await.result(Future.successful(Success(passage)), Duration.Inf))
        }

      }


    }


  }

  "delete" should {

    "passage with specified id is not found" should {

      trait CannotFindPassageScope extends PassageAuthScope {
        passageService.get(passageId) returns Future.successful(Success(None))
        access.grant(identity, Permission.Write, (passage, None)) returns Future.successful(Success(false))
        val result = Await.result(passageAuthWired.delete(passageId.toString)(identity, executionContext), Duration.Inf)
      }

      "return cantFindPassageWithId" in new CannotFindPassageScope {
        result must be equalTo Failure(cantFindPassageWithId(passageId))
      }

    }

    "passage with specified id is found" should {

      trait FoundPassageScope extends PassageAuthScope {
        passageService.get(passageId) returns Future.successful(Success(Some(passage)))
      }

      "user does not have permission to write passage" should {

        trait PermissionDeniedScope extends FoundPassageScope {
          access.grant(identity, Permission.Write, (passage, None)) returns Future.successful(Success(false))
          val result = Await.result(passageAuthWired.delete(passageId.toString)(identity, executionContext), Duration.Inf)
        }

        "return couldNotWritePassage error" in new PermissionDeniedScope {
          result must be equalTo Failure(couldNotWritePassage(passageId))
        }

      }

      "user does have permission to write passage" should {

        trait PermissionAllowed extends FoundPassageScope {
          access.grant(identity, Permission.Write, (passage, None)) returns Future.successful(Success(true))
        }

        "passageService#delete fails" should {

          trait PassageDeleteFails extends PermissionAllowed {
            passageService.delete(passageId) returns Future.successful(Failure(PassageSaveError(passageId)))
            val result = Await.result(passageAuthWired.delete(passageId.toString)(identity, executionContext), Duration.Inf)
          }

          "call passageService#delete" in new PassageDeleteFails {
            there was one(passageService).delete(passageId)
          }

          "return couldNotDeletePassage" in new PassageDeleteFails {
            result must be equalTo Failure(couldNotDeletePassage(passageId))
          }

        }

        "passageService#delete succeeds" should {

          trait PassageDeleteSucceeds extends PermissionAllowed {
            passageService.delete(passageId) returns Future.successful(Success(passage))
            val result = Await.result(passageAuthWired.delete(passageId.toString)(identity, executionContext), Duration.Inf)
          }

          "call passageService#delete" in new PassageDeleteSucceeds {
            there was one(passageService).delete(passageId)
          }

          "return the deleted passage" in new PassageDeleteSucceeds {
            result must be equalTo Success(passage)
          }

        }

      }

    }

  }

}
