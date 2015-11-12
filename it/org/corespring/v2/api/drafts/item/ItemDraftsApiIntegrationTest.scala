package org.corespring.v2.api.drafts.item

import com.novus.salat.Context
import org.corespring.drafts.item.ItemDraftHelper
import org.corespring.drafts.item.models.DraftId
import org.corespring.it.IntegrationSpecification
import org.corespring.it.helpers.SecureSocialHelper
import org.corespring.it.scopes.{ SessionRequestBuilder, userAndItem }
import org.corespring.services.item.ItemService
import org.corespring.test.SecureSocialHelpers
import org.corespring.v2.player.scopes.SessionRequestBuilder
import org.corespring.v2.player.scopes.userAndItem
import play.api.mvc.AnyContentAsEmpty
import play.api.test.PlaySpecification

class ItemDraftsApiIntegrationTest extends IntegrationSpecification with PlaySpecification {

  val routes = org.corespring.v2.api.drafts.item.routes.ItemDrafts

  lazy val helper = new ItemDraftHelper {
    override def itemService: ItemService = bootstrap.Main.itemService

    override implicit def context: Context = bootstrap.Main.context
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

      trait scope extends userAndItem with SessionRequestBuilder with SecureSocialHelpers

      "copy the item assets to the new item version, when creating from a published item with assets" in new scope {
        true === false
      }.pendingUntilFixed
    }
  }

}
