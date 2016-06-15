package org.corespring.v2.api.services

import org.corespring.v2.api.services.CachingPlayerDefinitionService.CacheType
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import org.bson.types.ObjectId
import org.corespring.errors.PlatformServiceError
import org.corespring.models.item.PlayerDefinition
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.item.PlayerDefinitionService
import play.api.test.{ DefaultAwaitTimeout, FutureAwaits }

import scala.concurrent.Future
import scalaz.{ Failure, Success, Validation }

class CachingPlayerDefinitionServiceTest extends Specification with Mockito with FutureAwaits with DefaultAwaitTimeout {

  import scala.concurrent.ExecutionContext.Implicits.global

  trait scope extends Scope {

    lazy val orgId = ObjectId.get
    lazy val itemOneId = VersionedId(ObjectId.get, Some(0))

    lazy val underLyingFailure = Failure(PlatformServiceError("underlying error"))

    lazy val underlying = {
      val m = mock[PlayerDefinitionService]
      m
    }

    lazy val cache = spray.caching.LruCache[CacheType]()

    lazy val service = new CachingPlayerDefinitionService(underlying, cache)(global)
  }

  "findMultiplePlayerDefinitions" should {

    trait callTwice extends scope {
      def result: Validation[PlatformServiceError, PlayerDefinition]

      underlying.findMultiplePlayerDefinitions(any[ObjectId], any[VersionedId[ObjectId]]) returns {
        Future.successful(Seq(itemOneId -> result))
      }
      val resultOne = await(service.findMultiplePlayerDefinitions(orgId, itemOneId))
      val resultTwo = await(service.findMultiplePlayerDefinitions(orgId, itemOneId))
    }
    trait callTwiceReturnFailure extends callTwice {
      override lazy val result = underLyingFailure
    }

    trait callTwiceReturnSuccess extends callTwice {
      override lazy val result = Success(PlayerDefinition.empty)
    }

    "when failing" should {
      "call the underlying service once" in new callTwiceReturnFailure {
        there was one(underlying).findMultiplePlayerDefinitions(any[ObjectId], any[VersionedId[ObjectId]])
      }

      "return the failure from the underlying service" in new callTwiceReturnFailure {
        resultOne must equalTo(Seq(itemOneId -> underLyingFailure))
      }

      "return the same result twice" in new callTwiceReturnFailure {
        resultOne must equalTo(resultTwo)
      }
    }

    "when successful" should {
      "call the underlying service once" in new callTwiceReturnSuccess {
        there was one(underlying).findMultiplePlayerDefinitions(any[ObjectId], any[VersionedId[ObjectId]])
      }

      "return the failure from the underlying service" in new callTwiceReturnSuccess {
        resultOne must equalTo(Seq(itemOneId -> Success(PlayerDefinition.empty)))
      }

      "return the same result twice" in new callTwiceReturnSuccess {
        resultOne must equalTo(resultTwo)
      }

    }
  }
}
