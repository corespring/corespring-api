package org.corespring.services.salat

import org.bson.types.ObjectId
import org.corespring.models.item.Passage
import org.corespring.models.item.resource.BaseFile
import org.corespring.platform.data.VersioningDao
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.salat.bootstrap.SalatServicesExecutionContext
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class PassageServiceTest extends Specification with Mockito {

  trait PassageServiceScope extends Scope {
    val dao = mock[VersioningDao[Passage, VersionedId[ObjectId]]]
    val executionContext = mock[SalatServicesExecutionContext]
    val passageService = new PassageService(dao, executionContext)

    val id = new VersionedId[ObjectId](new ObjectId(), Some(0))
    val collectionId = new ObjectId().toString
    val file = mock[BaseFile]

    val passage = Passage(id, collectionId = collectionId, file = file)
  }



  "get" should {

    "item does not exist" should {

      trait PassageMissingScope extends PassageServiceScope {
        dao.get(id) returns(None)
      }

      "return None" in new PassageMissingScope {
        Await.result(passageService.get(id), Duration.Inf) must beEmpty
      }

    }

    "item exists" should {

      trait PassageFoundScope extends PassageServiceScope {
        dao.get(id) returns(Some(passage))
      }

      "return Some(passage)" in new PassageFoundScope {
        Await.result(passageService.get(id), Duration.Inf) must beEqualTo(Some(passage))
      }

    }

  }


}
