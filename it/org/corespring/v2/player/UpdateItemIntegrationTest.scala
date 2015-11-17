package org.corespring.v2.player.item

import org.corespring.it.IntegrationSpecification
import org.corespring.platform.core.models.item.TaskInfo
import org.corespring.test.SecureSocialHelpers
import org.corespring.test.helpers.models.ItemHelper
import org.corespring.v2.player.scopes.{ SessionRequestBuilder, userAndItem }
import org.specs2.specification.Scope
import play.api.libs.json.Json
import play.api.mvc.{ Request, AnyContentAsJson }

class UpdateItemIntegrationTest extends IntegrationSpecification {

  trait scope extends Scope with userAndItem with SessionRequestBuilder with SecureSocialHelpers {

  }

  "update profile" should {
    "update taskInfo.title" in new scope {

      val taskInfo = TaskInfo()
      val call = org.corespring.container.client.controllers.resources.routes.Item.saveSubset(itemId.toString, "profile")
      val request: Request[AnyContentAsJson] = makeJsonRequest(call, Json.obj("taskInfo" -> Json.obj("title" -> "new title")))
      logger.debug(s"request: $request")
      logger.debug(s"body: ${request.body.json}")
      val result = route(request)(writeableOf_AnyContentAsJson).get
      logger.debug(s"result: ${contentAsString(result)}")
      status(result) must_== OK
      ItemHelper.get(itemId).get.taskInfo.flatMap(_.title) must_== Some("new title")
    }
  }
}
