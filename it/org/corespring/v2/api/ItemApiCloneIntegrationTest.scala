package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.assets.ItemAssetKeys
import org.corespring.it.assets.ImageUtils
import org.corespring.it.helpers.{ CollectionHelper, ItemHelper, OrganizationHelper }
import org.corespring.it.scopes.{ TokenRequestBuilder, orgWithAccessTokenAndItem }
import org.corespring.it.{ IntegrationSpecification, MultipartFormDataWriteable }
import org.corespring.models.item.resource.StoredFile
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2.player.supportingMaterials.withUploadFile
import play.api.libs.json.Json
import play.api.test.PlaySpecification

class ItemApiCloneIntegrationTest extends IntegrationSpecification with PlaySpecification {

  val routes = org.corespring.v2.api.routes.ItemApi

  "ItemApi" should {
    "when calling clone" should {

      trait clone extends orgWithAccessTokenAndItem with TokenRequestBuilder {

        def json = Json.obj()

        lazy val result = {
          val request = makeJsonRequest(routes.cloneItem(itemId.toString), json)
          route(request)(writeableOf_AnyContentAsJson).get
        }

        lazy val clonedItemId = VersionedId((contentAsJson(result) \ "id").as[String]).get

        override def after: Unit = {
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

      "clone an item with binary supporting materials" in new clone with withUploadFile {
        override def filePath: String = s"/test-images/puppy.small.jpg"

        override def after = {
          super.after
          ImageUtils.delete(ItemAssetKeys.folder(clonedItemId))
        }

        val item = ItemHelper.get(itemId).get
        val uploadCall = org.corespring.container.client.controllers.resources.routes.Item.createSupportingMaterialFromFile(itemId.toString)
        val formData = Map("name" -> "binary-material", "materialType" -> "Rubric")
        val form = mkFormWithFile(formData)
        val req = makeFormRequest(uploadCall, form)
        val uploadResult = route(req)(MultipartFormDataWriteable.writeableOf_multipartFormData).get
        status(uploadResult) must_== CREATED
        logger.debug(s"clone result: ${contentAsJson(result)}")
        status(result) must_== OK
        val keys = ImageUtils.list(ItemAssetKeys.folder(clonedItemId))
        keys(0).matches(ItemAssetKeys.supportingMaterialFile(clonedItemId, "binary-material", ".*?-puppy.small.jpg")) must_== true

        val material = ItemHelper.get(clonedItemId).get.supportingMaterials.find(_.name == "binary-material").get

        val foundFile = material.files.find {
          case StoredFile(_, "image/jpeg", true, "") => true
        }

        foundFile.map(_.name.contains("puppy.small.jpg")) must_== Some(true)
      }

      "return an error if an invalid collection id is passed in" in new clone {
        override val json = Json.obj("collectionId" -> "not-a-valid-object-id")
        status(result) must_== BAD_REQUEST
      }

      "return an error if an the org doesn't have write access to the item collection" in new clone {
        val otherOrg = OrganizationHelper.create()
        val otherCollectionId = CollectionHelper.create(otherOrg)
        override val json = Json.obj("collectionId" -> otherCollectionId.toString)
        status(result) must_== BAD_REQUEST
      }

    }
  }
}
