package org.corespring.v2.api

import developer.controllers.SecureSocialHelpers
import org.corespring.it.IntegrationSpecification
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2.player.scopes.{ SessionRequestBuilder, userAndItem }
import play.api.test.PlaySpecification

class ItemApiCloneIntegrationTest extends IntegrationSpecification with PlaySpecification {

  val routes = org.corespring.v2.api.routes.ItemApi

  "ItemApi" should {
    "when calling clone" should {

      trait clone extends userAndItem with SessionRequestBuilder with SecureSocialHelpers {

        lazy val result = {
          val request = makeRequest(routes.cloneItem(itemId.toString))
          route(request)(writeable)
        }

        lazy val clonedItemId = result.map { r => (contentAsJson(r) \ "id").as[String] }

        override def after = {
          super.after

          clonedItemId.map { id =>
            VersionedId(id).foreach(ItemHelper.delete)
          }
        }
      }

      "clone" in new clone {
        result.map { r =>
          status(r) === OK
        }
      }

    }
  }
}
