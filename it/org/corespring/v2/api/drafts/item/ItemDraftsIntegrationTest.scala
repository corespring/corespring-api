package org.corespring.v2.api.drafts.item

import org.corespring.it.IntegrationSpecification
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.test.SecureSocialHelpers
import org.corespring.test.helpers.models.ItemHelper
import org.corespring.v2.player.scopes.{SessionRequestBuilder, userAndItem}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.{FakeRequest, PlaySpecification}

class ItemDraftsIntegrationTest extends IntegrationSpecification with PlaySpecification {

  val routes = org.corespring.v2.api.drafts.item.routes.ItemDrafts

  "ItemApi" should {
    "commit" should {

      trait commitWithContentType extends userAndItem with SessionRequestBuilder with SecureSocialHelpers {

        val contentType:String = ""

        lazy val result = {
          val request = makeRequestWithContentType(routes.commit(itemId.toString), AnyContentAsEmpty, contentType)
          route(request)(writeable)
        }

      }

      "work when content-type-header is xml (ss AC-201)" in new commitWithContentType {
        override val contentType:String = "text/xml"

        result.map { r =>
          //check against parts of the output as status is not specific enough
          contentAsString(r) must not contain("[Invalid XML]")
        }
      }
    }
  }
}
