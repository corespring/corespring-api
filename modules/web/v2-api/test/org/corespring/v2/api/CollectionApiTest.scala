package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.services.ContentCollectionService
import org.corespring.services.item.ItemAggregationService
import org.corespring.v2.auth.models.{ AuthMode, OrgAndOpts }
import org.corespring.v2.errors.V2Error
import org.specs2.specification.Scope
import play.api.libs.json.{ Json }
import play.api.test.FakeRequest

import scala.concurrent.Future
import scalaz.{ Success, Validation }

class CollectionApiTest extends V2ApiSpec {

  import scala.concurrent.ExecutionContext.Implicits.global

  trait scope extends Scope with V2ApiScope {

    lazy val contentCollectionService: ContentCollectionService = {
      val m = mock[ContentCollectionService]
      m
    }

    lazy val itemAggregationService: ItemAggregationService = {
      val m = mock[ItemAggregationService]
      m.taskInfoItemTypeCounts(any[Seq[ObjectId]]) returns Future(Map("a" -> 1))
      m.contributorCounts(any[Seq[ObjectId]]) returns Future(Map("b" -> 1))
      m
    }

    val req = FakeRequest("", "")

    override def orgAndOpts: Validation[V2Error, OrgAndOpts] = Success(mockOrgAndOpts(AuthMode.AccessToken))

    val api = new CollectionApi(
      contentCollectionService,
      itemAggregationService,
      v2ApiContext,
      jsonFormatting,
      getOrgAndOptionsFn)
  }

  "fieldValuesByFrequency" should {

    trait fieldValues extends scope {
      val ids = Seq(ObjectId.get, ObjectId.get)
      val idsString = ids.map(_.toString).mkString(",")
    }

    "when calling taskInfoTypeCounts" should {

      trait itemType extends fieldValues {
        lazy val result = api.fieldValuesByFrequency(idsString, "itemType")(req)
      }

      "call taskInfoItemTypeCounts" in new itemType {
        await(result)
        there was one(itemAggregationService).taskInfoItemTypeCounts(ids)
      }

      "return the result" in new itemType {
        status(result) === OK
        (contentAsJson(result) \ "a").asOpt[Double] === Some(1)
      }
    }

    "when calling contributorCounts" should {

      trait contributor extends fieldValues {
        lazy val result = api.fieldValuesByFrequency(idsString, "contributor")(req)
      }

      "call contributorCounts" in new contributor {
        await(result)
        there was one(itemAggregationService).contributorCounts(ids)
      }

      "return the result" in new contributor {
        status(result) === OK
        (contentAsJson(result) \ "b").asOpt[Double] === Some(1)
      }
    }

    "return an empty list for an unknown field type" in new fieldValues {
      val result = api.fieldValuesByFrequency(idsString, "?")(req)
      contentAsJson(result) === Json.obj()
    }

  }
}
