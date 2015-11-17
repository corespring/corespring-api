package org.corespring.drafts

import com.novus.salat.Context
import org.bson.types.ObjectId
import org.corespring.drafts.item.{ S3Paths, ItemDraftHelper }
import org.corespring.drafts.item.models.DraftId
import org.corespring.it.IntegrationSpecification
import org.corespring.it.assets.ImageUtils
import org.corespring.it.helpers.{ ItemHelper, OrganizationHelper, SecureSocialHelper }
import org.corespring.it.scopes.{ SessionRequestBuilder, userAndItem }
import org.corespring.models.item.FieldValue
import org.corespring.models.item.resource.StoredFile
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.item.ItemService
import org.specs2.specification.Scope
import play.api.http.Writeable
import play.api.libs.json.JsArray
import play.api.libs.json.Json._
import play.api.mvc.Request

/**
 * Illustrates the bug: https://thesib.atlassian.net/browse/PE-408
 */
class ItemDraftsCreatePublishIntegrationTest extends IntegrationSpecification {

  val itemDraftHelper = new ItemDraftHelper {
    override implicit def context: Context = bootstrap.Main.context

    override def itemService: ItemService = bootstrap.Main.itemService
  }

  trait scope extends Scope with userAndItem with SessionRequestBuilder with SecureSocialHelper {

    val expectedFiles = Seq(StoredFile("ervin.png", "image/png", false, "ervin.png"))

    def r[A](r: Request[A])(implicit w: Writeable[A]) = {
      val f = route(r)(w).get
      logger.debug(s"${r.path} - status: ${status(f)}")
      logger.trace(s"${r.path} - content: ${contentAsString(f)}")
      f
    }

    def initFieldValues() = {
      bootstrap.Main.fieldValueService.insert(FieldValue())
    }

    def createDraft() = {
      val org = OrganizationHelper.service.findOneById(orgId).get
      val draftId = DraftId(itemId.id, user.userName, orgId)
      itemDraftHelper.create(draftId, itemId, org)
    }

    def uploadAsset() = {
      val call = org.corespring.container.client.controllers.apps.routes.DraftEditor.uploadFile(draftId.toIdString, "ervin.png")
      val bytes = ImageUtils.imageData("/test-images/ervin.png")
      val request = makeRawRequest(call, bytes)
      r(request)
    }

    def updateWholeItem() = {
      val call = org.corespring.container.client.controllers.resources.routes.ItemDraft.save(draftId.toIdString)
      val data = obj(
        "profile" -> obj(
          "taskInfo" -> obj(
            "tile" -> "test item")),
        "files" -> JsArray(Seq.empty))

      val request = makeJsonRequest(call, data)
      r(request)(writeableOf_AnyContentAsJson)
    }

    def commitDraft() = {
      val call = org.corespring.v2.api.drafts.item.routes.ItemDrafts.commit(draftId.toIdString)
      val request = makeRequest(call)
      r(request)
    }

    initFieldValues()

    //1. create the draft
    val draftId = createDraft()

    //2. upload an asset
    val uploadResult = uploadAsset()
    require(status(uploadResult) == OK)

    val updateResult = updateWholeItem()
    require(status(updateResult) == OK)

    val commitResult = commitDraft()
    require(status(commitResult) === OK)
  }

  "when uploading an asset, updating the whole draft and then committing a draft" should {
    "add the asset data to the model" in new scope {
      lazy val item = ItemHelper.get(itemId).get
      item.playerDefinition.get.files must_== expectedFiles
    }

    "copy the assets from the draft over to the item" in new scope {
      lazy val item = ItemHelper.get(itemId).get
      val keys = ImageUtils.list(S3Paths.itemFolder(itemId))
      logger.debug(s"keys: $keys")
      keys(0) must_== S3Paths.itemFile(itemId, "ervin.png")
    }
  }

  """when uploading an asset,
    updating the whole draft,
    committing the draft,
    publishing the item,
    then creating a new draft from the published item""" should {

    trait published extends scope {

      def publishItem() = {
        val call = org.corespring.v2.api.routes.ItemApi.publish(itemId.toString)
        val request = makeRequest(call)
        r(request)
      }

      def getNewDraft() = {
        val call = org.corespring.v2.api.drafts.item.routes.ItemDrafts.create(itemId.toString)
        val request = makeRequest(call)
        r(request)
      }

      val publishResult = publishItem()
      require(status(publishResult) == OK)

      val createNewDraftResult = getNewDraft()
      require(status(createNewDraftResult) == OK)

      val newId: VersionedId[ObjectId] = itemId.copy(version = Some(1))
    }

    "have the item assets in the model of latest version of the item" in new published {
      val newItem = ItemHelper.get(newId).get
      newItem.playerDefinition.get.files must_== Seq(StoredFile("ervin.png", "image/png", false, "ervin.png"))
    }

    "have the item assets stored on s3 for latest version of the item" in new published {
      val keys = ImageUtils.list(S3Paths.itemFolder(newId))
      logger.debug(s"keys: $keys")
      keys(0) must_== S3Paths.itemFile(newId, "ervin.png")
    }

    "have the item assets in the model of the new draft" in new published {
      val draft = itemDraftHelper.get(draftId).get
      draft.change.data.playerDefinition.get.files must_== expectedFiles
    }

    "have the item assets stored on s3 for the new draft" in new published {
      val keys = ImageUtils.list(S3Paths.draftFolder(draftId))
      logger.debug(s"keys: $keys")
      keys(0) must_== S3Paths.draftFile(draftId, "ervin.png")
    }
  }
}
