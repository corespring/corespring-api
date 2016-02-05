package org.corespring.services.salat

import com.mongodb.DBObject
import org.bson.types.ObjectId
import org.corespring.errors.{PassageSaveError, PassageInsertError, PassageReadError}
import org.corespring.models.appConfig.ArchiveConfig
import org.corespring.models.item.Passage
import org.corespring.models.item.resource.BaseFile
import org.corespring.platform.data.VersioningDao
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.salat.bootstrap.SalatServicesExecutionContext
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import scala.concurrent.duration.Duration
import scala.concurrent.{Future, ExecutionContext, Await}
import scalaz.{Failure, Success}

class PassageServiceTest extends Specification with Mockito {

  trait PassageServiceScope extends Scope {
    val dao = mock[VersioningDao[Passage, VersionedId[ObjectId]]]
    val executionContext = SalatServicesExecutionContext(ExecutionContext.global)
    val archiveCollId = new ObjectId()
    val archiveConfig = mock[ArchiveConfig]
    archiveConfig.contentCollectionId returns archiveCollId
    val passageService = new PassageService(dao, archiveConfig, executionContext)

    val passageId = new VersionedId[ObjectId](new ObjectId(), Some(0))
    val collectionId = new ObjectId().toString
    val file = mock[BaseFile]

    val passage = Passage(passageId, collectionId = collectionId, file = file)

    val exception = new Exception("Someone set the database on fire!")
  }

  trait PassageMissingScope extends PassageServiceScope {
    dao.get(passageId) returns(None)
  }

  "get" should {

    "item does not exist" should {

      "return None" in new PassageMissingScope {
        Await.result(passageService.get(passageId), Duration.Inf) must beEqualTo(Success(None))
      }

    }

    "item exists" should {

      trait PassageFoundScope extends PassageServiceScope {
        dao.get(passageId) returns(Some(passage))
      }

      "return Some(passage)" in new PassageFoundScope {
        Await.result(passageService.get(passageId), Duration.Inf) must beEqualTo(Success(Some(passage)))
      }

    }

  }

  "insert" should {

    "dao#insert succeeds" should {

      trait DaoSuccess extends PassageServiceScope {
        dao.insert(passage) returns Some(passageId)
      }

      "return passage" in new DaoSuccess {
        Await.result(passageService.insert(passage), Duration.Inf) must be equalTo(Success(passage))
      }

    }

    "dao#insert fails" should {

      trait DaoFailure extends PassageServiceScope {
        dao.insert(passage) answers { _ => throw exception }
      }

      "return PassageInsertError" in new DaoFailure {
        Await.result(passageService.insert(passage), Duration.Inf) must be equalTo(
         Failure(PassageInsertError(Some(exception))))
      }

    }

  }

  "save" should {

    "dao#save succeeds" should {

      trait PassageSaveSucceeds extends PassageServiceScope {
        dao.save(passage, false) returns Right(passageId)
      }

      "return passage" in new PassageSaveSucceeds {
        Await.result(passageService.save(passage), Duration.Inf) must be equalTo(Success(passage))
      }

    }

    "dao#save fails" should {

      trait PassageSaveFails extends PassageServiceScope {
        dao.save(passage, false) answers { _ => throw exception }
        val result = Await.result(passageService.save(passage), Duration.Inf)
      }

      "return error" in new PassageSaveFails {
        result must be equalTo(Failure(PassageSaveError(passage.id, Some(exception))))
      }

    }

  }

  "delete" should {

    "item does not exist" should {

      "return PassageReadError" in new PassageMissingScope {
        Await.result(passageService.delete(passageId), Duration.Inf) must beEqualTo(Failure(PassageReadError(passageId)))
      }

    }

    "dao to get returns error" should {

      trait PassageGetFails extends PassageServiceScope {
        dao.get(passageId) answers { _ => throw exception }
        dao.update(any[VersionedId[ObjectId]], any[DBObject], any[Boolean]) returns Right(passageId)
      }

      "return PassageReadError" in new PassageGetFails {
        Await.result(passageService.delete(passageId), Duration.Inf) must beEqualTo(Failure(PassageReadError(passageId, Some(exception))))
      }
    }

    "dao#get returns None" should {

      trait PassageGetNone extends PassageServiceScope {
        dao.get(passageId) returns None
      }

      "return a PassageReadError" in new PassageGetNone {
        Await.result(passageService.delete(passageId), Duration.Inf) must be equalTo(Failure(PassageReadError(passageId)))
      }

    }

    "dao#get returns passage" should {

      trait PassageGetSucceeds extends PassageServiceScope {
        dao.get(passageId) returns Some(passage)
        val result = Await.result(passageService.delete(passageId), Duration.Inf)
      }

      "call dao#update" in new PassageGetSucceeds {
        there was one(dao).update(any[VersionedId[ObjectId]], any[DBObject], any[Boolean])
      }

      "return passage with archived collection id" in new PassageGetSucceeds {
        result must be equalTo(Success(passage.copy(collectionId = archiveCollId.toString)))
      }

    }

  }


}
