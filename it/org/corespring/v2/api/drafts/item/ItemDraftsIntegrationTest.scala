package org.corespring.v2.api.drafts.item

import org.corespring.it.IntegrationSpecification
import org.corespring.it.helpers.SecureSocialHelper
import org.corespring.it.scope.scopes.{ SessionRequestBuilder, userAndItem }
import play.api.mvc.AnyContentAsEmpty
import play.api.test.PlaySpecification

class ItemDraftsIntegrationTest extends IntegrationSpecification with PlaySpecification {

  val routes = org.corespring.v2.api.drafts.item.routes.ItemDrafts

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
  }
}
