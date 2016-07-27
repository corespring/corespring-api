package org.corespring.v2.player.item

import org.corespring.it.{ FieldValuesIniter, IntegrationSpecification }
import org.corespring.models.item.TaskInfo
import org.corespring.it.helpers.SecureSocialHelper
import org.corespring.it.helpers.ItemHelper
import org.corespring.it.scopes.{ SessionRequestBuilder, userAndItem }
import org.specs2.specification.Scope
import play.api.libs.json.Json
import play.api.mvc.{ Request, AnyContentAsJson }

class UpdateItemIntegrationTest extends IntegrationSpecification {

  trait scope extends Scope with userAndItem with SessionRequestBuilder with SecureSocialHelper with FieldValuesIniter {

    initFieldValues()

    override def after = {
      logger.debug(" ----------- >> after.. cleaning up..")
      removeData()
    }
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

  "saveXhtmlAndComponents" should {

    trait saveXhtmlAndComponents extends scope {
      lazy val call = org.corespring.container.client.controllers.resources.routes.Item.saveXhtmlAndComponents(itemId.toString)
      val components = Json.obj("1" -> Json.obj("componentType" -> "my-comp"))
      val xhtml = "<div>hi there</div><div id=\"1\"></div>"
      lazy val request: Request[AnyContentAsJson] = makeJsonRequest(call, Json.obj("xhtml" -> xhtml, "components" -> components))
      logger.debug(s"request: $request")
      logger.debug(s"body: ${request.body.json}")
      lazy val result = route(request)(writeableOf_AnyContentAsJson).get
      logger.debug(s"result: ${contentAsString(result)}")
      status(result) must_== OK
    }

    "update the xhtml" in new saveXhtmlAndComponents {
      ItemHelper.get(itemId).get.playerDefinition.map(_.xhtml) must_== Some(xhtml)
    }

    "update the components" in new saveXhtmlAndComponents {
      ItemHelper.get(itemId).get.playerDefinition.map(_.components) must_== Some(components)
    }
  }
}
