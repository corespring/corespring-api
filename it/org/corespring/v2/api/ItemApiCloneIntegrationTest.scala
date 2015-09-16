package org.corespring.v2.api

import org.corespring.it.IntegrationSpecification
import org.corespring.it.helpers.{ SecureSocialHelper, ItemHelper }
import org.corespring.it.scope.scopes.{ SessionRequestBuilder, userAndItem }
import org.corespring.platform.data.mongo.models.VersionedId
import play.api.test.PlaySpecification

class ItemApiCloneIntegrationTest extends IntegrationSpecification with PlaySpecification {

  val routes = org.corespring.v2.api.routes.ItemApi

  "ItemApi" should {
    "when calling clone" should {

      trait clone extends userAndItem with SessionRequestBuilder with SecureSocialHelper {

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
