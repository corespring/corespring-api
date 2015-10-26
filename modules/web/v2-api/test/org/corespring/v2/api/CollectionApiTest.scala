package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.models.auth.Permission
import org.corespring.models.{ ContentCollection, ContentCollRef, Organization }
import org.corespring.services.{ ShareItemWithCollectionsService, OrgCollectionService, ContentCollectionUpdate, ContentCollectionService }
import org.corespring.services.item.ItemAggregationService
import org.corespring.v2.auth.models.{ AuthMode, OrgAndOpts }
import org.corespring.v2.errors.V2Error
import org.specs2.specification.Scope
import play.api.libs.json.{ Json }
import play.api.mvc.{ AnyContent, Request }
import play.api.test.FakeRequest

import scala.concurrent.Future
import scalaz.{ Success, Validation }

class CollectionApiTest extends V2ApiSpec {

  import scala.concurrent.ExecutionContext.Implicits.global

  trait scope extends Scope with V2ApiScope {

    def toTuple[A, B, C](args: Any): (A, B, C) = {
      val arr = args.asInstanceOf[Array[Any]]
      (arr(0).asInstanceOf[A], arr(1).asInstanceOf[B], arr(2).asInstanceOf[C])
    }

    def toTuple[A, B](args: Any): (A, B) = {
      val arr = args.asInstanceOf[Array[Any]]
      (arr(0).asInstanceOf[A], arr(1).asInstanceOf[B])
    }
    lazy val orgCollectionService: OrgCollectionService = {
      val m = mock[OrgCollectionService]

      m.isAuthorized(any[ObjectId], any[ObjectId], any[Permission]) returns true

      m.ownsCollection(any[Organization], any[ObjectId]) returns Success(true)

      m.enableOrgAccessToCollection(any[ObjectId], any[ObjectId]) answers { (args, _) =>
        val (_, collectionId) = toTuple[ObjectId, ObjectId](args)
        Success(ContentCollRef(collectionId, Permission.Read.value, true))
      }

      m.disableOrgAccessToCollection(any[ObjectId], any[ObjectId]) answers { (args, _) =>
        val (_, collectionId) = toTuple[ObjectId, ObjectId](args)
        Success(ContentCollRef(collectionId, Permission.Read.value, false))
      }

      m.grantAccessToCollection(any[ObjectId], any[ObjectId], any[Permission]) answers { (args, _) =>
        val (collectionId, _, permission) = toTuple[ObjectId, ObjectId, Permission](args)
        Success(Organization("test"))
      }
      m
    }

    lazy val shareItemWithCollectionService: ShareItemWithCollectionsService = {
      val m = mock[ShareItemWithCollectionsService]
      m
    }

    lazy val contentCollectionService: ContentCollectionService = {
      val m = mock[ContentCollectionService]

      m.delete(any[ObjectId]) returns Success()

      m.update(any[ObjectId], any[ContentCollectionUpdate]) answers { (args, _) =>
        val (collectionId, update) = toTuple[ObjectId, ContentCollectionUpdate](args)
        Success(
          ContentCollection(
            id = collectionId,
            ownerOrgId = orgId,
            name = update.name.getOrElse("?"),
            isPublic = update.isPublic.getOrElse(false)))
      }
      m
    }

    lazy val itemAggregationService: ItemAggregationService = {
      val m = mock[ItemAggregationService]
      m.taskInfoItemTypeCounts(any[Seq[ObjectId]]) returns Future(Map("a" -> 1))
      m.contributorCounts(any[Seq[ObjectId]]) returns Future(Map("b" -> 1))
      m
    }

    def req: Request[AnyContent] = FakeRequest("", "")

    protected val collectionId = mockCollectionId()

    protected val stubbedOrgAndOpts: OrgAndOpts = {
      val stub = mockOrgAndOpts(AuthMode.AccessToken)
      stub.copy(org = stub.org.copy(contentcolls = stub.org.contentcolls :+ ContentCollRef(collectionId, Permission.Write.value, true)))
    }

    override val orgAndOpts: Validation[V2Error, OrgAndOpts] = Success(stubbedOrgAndOpts)
    protected val orgId = stubbedOrgAndOpts.org.id

    val api = new CollectionApi(
      shareItemWithCollectionService,
      orgCollectionService,
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

  "shareCollection" should {

    trait shareCollection extends scope {
      val result = api.shareCollection(collectionId, orgId)(req)
      await(result)
    }

    "call contentCollectionService.ownsCollection" in new shareCollection {
      there was one(orgCollectionService).ownsCollection(any[Organization], any[ObjectId])
    }

    "call contentCollectionService.shareCollectionWithOrg" in new shareCollection {
      there was one(orgCollectionService).grantAccessToCollection(any[ObjectId], any[ObjectId], any[Permission])
    }

    "return the id of the updated collection" in new shareCollection {
      contentAsJson(result) === Json.obj("updated" -> collectionId.toString)
    }
  }

  "setEnabledStatus" should {

    trait setEnabledStatus extends scope {
      def enabled: Boolean = true
      lazy val result = api.setEnabledStatus(collectionId, enabled)(req)
      await(result)
    }

    "call contentCollectionService.enableOrgAccessToCollectionForOrg" in new setEnabledStatus {
      there was one(orgCollectionService).enableOrgAccessToCollection(orgId, collectionId)
    }

    "call contentCollectionService.disableOrgAccessToCollectionForOrg" in new setEnabledStatus {
      override lazy val enabled = false
      there was one(orgCollectionService).disableOrgAccessToCollection(orgId, collectionId)
    }

    "return the id of the updated collection" in new setEnabledStatus {
      contentAsJson(result) === Json.obj("updated" -> collectionId.toString)
    }
  }

  "deleteCollection" should {

    trait deleteCollection extends scope {
      val result = api.deleteCollection(collectionId)(req)
      await(result)
    }

    "call contentCollectionService.delete" in new deleteCollection {
      there was one(contentCollectionService).delete(collectionId)
    }

    "returns an empty json response" in new deleteCollection {
      contentAsJson(result) === Json.obj()
    }

  }

  "updateCollection" should {

    trait updateCollection extends scope {

      override lazy val req = FakeRequest("", "").withJsonBody(Json.obj())
      lazy val result = api.updateCollection(collectionId)(req)
      await(result)
    }

    "return a failure if the json body has 'organizations'" in new updateCollection {
      override lazy val req = FakeRequest("", "").withJsonBody(Json.obj("organizations" -> Json.obj()))
      status(result) === NOT_IMPLEMENTED
    }

    "call contentCollectionService.update with name only in update" in new updateCollection {
      override lazy val req = FakeRequest("", "").withJsonBody(Json.obj("name" -> "new-name"))
      there was one(contentCollectionService).update(collectionId, ContentCollectionUpdate(Some("new-name"), None))
    }

    "call contentCollectionService.update with isPublic only in update" in new updateCollection {
      override lazy val req = FakeRequest("", "").withJsonBody(Json.obj("isPublic" -> false))
      there was one(contentCollectionService).update(collectionId, ContentCollectionUpdate(None, Some(false)))
    }

    "call contentCollectionService.update with name and isPublic in update" in new updateCollection {
      override lazy val req = FakeRequest("", "").withJsonBody(Json.obj("isPublic" -> false, "name" -> "new-name"))
      there was one(contentCollectionService).update(collectionId, ContentCollectionUpdate(Some("new-name"), Some(false)))
    }

    "return the update collection" in new updateCollection {
      override lazy val req = FakeRequest("", "").withJsonBody(Json.obj("isPublic" -> true, "name" -> "new-name"))
      implicit val writes = org.corespring.models.json.ContentCollectionWrites
      contentAsJson(result) === Json.toJson(ContentCollection("new-name", orgId, true, collectionId))
    }
  }

  "shareFilteredItemsWithCollection" should {
    s"return $NOT_IMPLEMENTED" in new scope {
      status(api.shareFilteredItemsWithCollection(collectionId, None)(req)) === NOT_IMPLEMENTED
    }
  }
}
