package org.corespring.v2.api.drafts.item

import global.Global
import salat.Context
import org.corespring.assets.ItemAssetKeys
import org.corespring.drafts.item.ItemDraftHelper
import org.corespring.drafts.item.models.DraftId
import org.corespring.it.IntegrationSpecification
import org.corespring.it.assets.ImageUtils
import org.corespring.it.helpers.{ ItemHelper, SecureSocialHelper }
import org.corespring.it.scopes.{ SessionRequestBuilder, userAndItem }
import org.corespring.models.item.PlayerDefinition
import org.corespring.models.item.resource.StoredFile
import org.corespring.services.item.ItemService
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsEmpty
import play.api.test.PlaySpecification

class ItemDraftsApiIntegrationTest extends IntegrationSpecification with PlaySpecification {

  val routes = org.corespring.v2.api.drafts.item.routes.ItemDrafts

  lazy val helper = new ItemDraftHelper {
    override def itemService: ItemService = main.itemService

    override implicit def context: Context = main.context
  }

  "ItemDrafts" should {
    "commit" should {

      trait commitWithContentType extends userAndItem with SessionRequestBuilder with SecureSocialHelper {

        val contentType: String = ""

        lazy val result = {
          val request = makeRequestWithContentType(routes.commit(itemId.toString), AnyContentAsEmpty, contentType)
          route(request)(writeable)
        }

      }

      "not try to parse body as xml and fail bc body is empty, when content-type-header is xml (see AC-201)" in new commitWithContentType {
        override val contentType: String = "text/xml"

        result.map { r =>
          //check against parts of the output as status is not specific enough
          contentAsString(r) must not contain ("[Invalid XML]")
        }
      }
    }

    "listByItem" should {

      trait scope extends userAndItem with SessionRequestBuilder with SecureSocialHelper

      "return a list of draft headers" in new scope {
        helper.create(DraftId(itemId.id, "name", orgId), itemId, organization)
        val call = routes.listByItem(itemId.toString)
        val request = makeRequest(call)
        route(request)(writeable).map { r =>
          logger.debug(s"result=${contentAsString(r)}")
          (contentAsJson(r) \\ "itemId").map(_.as[String]) === Seq(itemId.id.toString)
          status(r) === OK
        }.getOrElse(ko)
      }
    }

    "create" should {

      trait scope extends userAndItem with SessionRequestBuilder with SecureSocialHelper {
        def addImageToModel: Boolean
        val path = "/test-images/ervin.png"
        val img = ImageUtils.resourcePathToFile(path)
        lazy val bytes = ImageUtils.imageData(path)
        lazy val s3Path: String = ItemAssetKeys.file(itemId, "ervin.png")
        ImageUtils.upload(img, s3Path)

        private def addFile(key: String)(pd: PlayerDefinition): PlayerDefinition = {
          val files = pd.files :+ StoredFile("ervin.png", "image/png", false)
          PlayerDefinition(files, pd.xhtml, pd.components, pd.summaryFeedback, pd.customScoring, pd.config)
        }

        lazy val updatedItem = {
          val item = ItemHelper.get(itemId).get
          val pd = if (addImageToModel) {
            item.playerDefinition.map(addFile(s3Path)).orElse(Some(addFile(s3Path)(PlayerDefinition.empty)))
          } else item.playerDefinition
          item.copy(published = true, playerDefinition = pd)
        }

        ItemHelper.update(updatedItem)

        lazy val call = routes.create(itemId.toString)
        lazy val result = route(makeRequest(call)).get
        lazy val resultJson = contentAsJson(result)
      }

      """when creating a draft from a published item with assets""" should {
        s"return $OK" in new scope {
          override def addImageToModel: Boolean = false
          status(result) must_== OK
        }

        s"returns the id as an id string" in new scope {
          override def addImageToModel: Boolean = false
          (contentAsJson(result) \ "id").asOpt[String] must_== Some(DraftId(itemId.id, user.userName, orgId).toIdString)
        }

        trait loadDraftAsset extends scope {
          logger.debug(s"json: ${Json.prettyPrint(resultJson)}")
          lazy val draftId = (resultJson \ "id").as[String]
          logger.debug(s"draftId: $draftId")
          import org.corespring.container.client.controllers.apps.routes.DraftEditor
          lazy val getFileCall = DraftEditor.getFile(draftId, "ervin.png")
          logger.debug(s"url: ${getFileCall.url}")
          lazy val getFileResult = route(makeRequest(getFileCall)).get
          lazy val resultStatus = status(getFileResult)

          if (resultStatus != OK) {
            logger.debug(s"result as string: ${contentAsString(getFileResult)}")
          }

          logger.debug(s"itemId: $itemId")
        }

        "when the image is added to the model, the draft should be able to load the asset from the new unpublished item" in new loadDraftAsset {
          override def addImageToModel: Boolean = true
          ImageUtils.exists(s3Path)
          resultStatus must_== OK
        }

      }

    }
  }

}
