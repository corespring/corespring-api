package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.it.IntegrationSpecification
import org.corespring.it.helpers.{ OrganizationHelper, CollectionHelper, SecureSocialHelper, ItemHelper }
import org.corespring.it.scopes.{ SessionRequestBuilder, userAndItem }
import org.corespring.platform.data.mongo.models.VersionedId
import play.api.libs.json.Json
import play.api.test.PlaySpecification

class ItemApiCloneIntegrationTest extends IntegrationSpecification with PlaySpecification {

  val routes = org.corespring.v2.api.routes.ItemApi

  "ItemApi" should {
    "when calling clone" should {

      trait clone extends userAndItem with SessionRequestBuilder with SecureSocialHelper {

        def json = Json.obj()

        lazy val result = {
          val request = makeJsonRequest(routes.cloneItem(itemId.toString), json)
          route(request)(writeableOf_AnyContentAsJson).get
        }

        lazy val clonedItemId = VersionedId((contentAsJson(result) \ "id").as[String]).get

        override def after = {
          super.after
          ItemHelper.delete(clonedItemId)
        }
      }

      s"return $OK" in new clone {
        status(result) === OK
      }

      """return {"id": "the-id"}""" in new clone {
        (contentAsJson(result) \ "id").asOpt[String] must_== Some(_: String)
      }

      """return a valid versioned id string: {"id": "..."}""" in new clone {
        (contentAsJson(result) \ "id").asOpt[String].flatMap(VersionedId(_)) must_== Some(_: VersionedId[ObjectId])
      }

      "clone an item to the same collection" in new clone {
        val item = ItemHelper.get(itemId).get
        ItemHelper.get(clonedItemId).get.collectionId must_== item.collectionId
      }

      "clone an item to a different collection" in new clone {
        val otherCollectionId = CollectionHelper.create(orgId)
        override val json = Json.obj("collectionId" -> otherCollectionId.toString)
        val item = ItemHelper.get(itemId).get
        ItemHelper.get(clonedItemId).get.collectionId must_== otherCollectionId.toString
      }

      "return an error if an invalid collection id is passed in" in new clone {
        override val json = Json.obj("collectionId" -> "not-a-valid-object-id")
        println(contentAsString(result))
        status(result) must_== BAD_REQUEST
      }

      "return an error if an the org doesn't have write access to the item collection" in new clone {
        val otherOrg = OrganizationHelper.create()
        val otherCollectionId = CollectionHelper.create(otherOrg)
        override val json = Json.obj("collectionId" -> otherCollectionId.toString)
        println(contentAsString(result))
        status(result) must_== BAD_REQUEST
      }

    }
  }
}
