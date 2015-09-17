package org.corespring.v2.api

import org.corespring.it.IntegrationSpecification
import org.corespring.it.helpers.ItemHelper
import org.corespring.it.scopes.orgWithAccessTokenAndItem
import play.api.test.FakeRequest

class ItemSessionApiUpdateV2JsonIntegrationTest extends IntegrationSpecification {

  class orgWithTokenAndTwoItems extends orgWithAccessTokenAndItem {

    lazy val itemService = bootstrap.Main.itemService
    val secondItemId = {
      println(s"find: $itemId")
      itemService.findOneById(itemId).map { item =>
        itemService.save(item, true)
        val out = itemId.copy(itemId.id, itemId.version.map { v => v + 1 })
        println(s"new item id: $out")
        out
      }.getOrElse {
        throw new RuntimeException("Can't create second item")
      }
    }

    val item = itemService.findOneById(itemId).getOrElse {
      throw new RuntimeException("Can't find item")
    }

    override def after: Any = {
      super.after
      ItemHelper.delete(secondItemId)
    }
  }

  "v2 - ItemSessionApi" should {
    "not throw an error when updating v2 json on a 'versioned' item" in new orgWithTokenAndTwoItems {

      val call = org.corespring.v2.api.routes.ItemSessionApi.create(itemId)
      route(FakeRequest(call.method, s"{call.url}?access_token=$accessToken")).map { result =>
        status(result) === OK
      }
    }

  }

}
