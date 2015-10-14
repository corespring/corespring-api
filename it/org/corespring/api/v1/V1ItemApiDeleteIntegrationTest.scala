package org.corespring.api.v1

import org.corespring.it.IntegrationSpecification
import org.corespring.it.helpers.ItemHelper
import org.corespring.it.scopes.{ orgWithAccessTokenAndItem, TokenRequestBuilder }
import play.api.test.PlaySpecification

class V1ItemApiDeleteIntegrationTest extends IntegrationSpecification with PlaySpecification {

  val Routes = org.corespring.api.v1.routes.ItemApi

  trait scope extends orgWithAccessTokenAndItem with TokenRequestBuilder {

  }

  "delete" should {

    "move the item to the archiveCollection" in new scope {

      val call = Routes.delete(itemId)

      val request = makeRequest(call)

      val item = ItemHelper.get(itemId)

      println(s"item collection id : ${item.get.collectionId}")
      println(s"our id: $collectionId")
      route(request).map { r =>

        println(contentAsString(r))
        status(r) === OK
      }
    }
  }
}
