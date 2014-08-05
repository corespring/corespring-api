package org.corespring.api.v1

import java.util.concurrent.TimeUnit

import org.corespring.it.IntegrationSpecification
import org.corespring.platform.core.models.item.TaskInfo
import org.corespring.platform.core.services.item.ItemServiceWired
import org.corespring.test.helpers.models.ItemHelper
import org.corespring.v2.player.scopes.orgWithAccessTokenAndItem
import play.api.libs.json.Json
import play.api.mvc.{ AnyContentAsJson, SimpleResult }
import play.api.test.{ FakeHeaders, FakeRequest }

import scala.concurrent.{ ExecutionContext, Future }

class ItemApiUpdateV2JsonTest extends IntegrationSpecification {

  class orgWithTokenAndTwoItems extends orgWithAccessTokenAndItem {

    lazy val secondItemId = {
      println(s"find: $itemId")
      ItemServiceWired.findOneById(itemId).map { item =>
        ItemServiceWired.save(item, true)
        val out = itemId.copy(itemId.id, itemId.version.map { v => v + 1 })
        println(s"new item id: $out")
        out
      }.getOrElse {
        throw new RuntimeException("Can't create second item")
      }
    }

    lazy val item = ItemServiceWired.findOneById(itemId).getOrElse {
      throw new RuntimeException("Can't find item")
    }

    override def after: Any = {
      super.after
      ItemHelper.delete(secondItemId)
    }
  }

  "ItemApi" should {
    "be able to call update on a versioned item - and it will be saved" in new orgWithTokenAndTwoItems {

      /**
       * Don't delete - this invocation is required to build these items
       * There is some weird timing issue that means we can't make these vals
       * will investigate
       */
      secondItemId
      item

      import ExecutionContext.Implicits.global

      val call = org.corespring.api.v1.routes.ItemApi.update(itemId)

      val itemJson = Json.toJson(item.copy(taskInfo = Some(TaskInfo(title = Some("new title")))))
      val maybeFuture = route(
        FakeRequest(
          call.method,
          s"${call.url}?access_token=${accessToken}",
          FakeHeaders(),
          AnyContentAsJson(itemJson)))

      val future = maybeFuture.getOrElse(Future(BadRequest("?")))

      import scala.concurrent.duration._

      val r = await[SimpleResult](future)(3.seconds)
      route(FakeRequest(call.method, s"${call.url}?access_token=${accessToken}")).map { result =>
        println(contentAsString(result))
        status(result) === OK
      }

    }

  }
}
