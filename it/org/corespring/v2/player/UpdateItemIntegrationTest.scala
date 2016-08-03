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

  "saveConfigXhtmlAndComponents" should {

    trait saveConfigXhtmlAndComponents extends scope {
      lazy val call = org.corespring.container.client.controllers.resources.routes.Item.saveConfigXhtmlAndComponents(itemId.toString)
      val components = Json.obj("1" -> Json.obj("componentType" -> "my-comp"))
      val config = Json.obj("prop" -> "value")
      val xhtml = "<div id=\"1\">hi there</div>"
      lazy val request: Request[AnyContentAsJson] = makeJsonRequest(call, Json.obj("config" -> config, "xhtml" -> xhtml, "components" -> components))
      logger.debug(s"request: $request")
      logger.debug(s"body: ${request.body.json}")
      lazy val result = route(request)(writeableOf_AnyContentAsJson).get
      logger.debug(s"result: ${contentAsString(result)}")
      status(result) must_== OK
    }

    "update the xhtml" in new saveConfigXhtmlAndComponents {
      ItemHelper.get(itemId).get.playerDefinition.map(_.xhtml) must_== Some(xhtml)
    }

    "update the config" in new saveConfigXhtmlAndComponents {
      ItemHelper.get(itemId).get.playerDefinition.map(_.config) must_== Some(config)
    }

    "update the components" in new saveConfigXhtmlAndComponents {
      ItemHelper.get(itemId).get.playerDefinition.map(_.components) must_== Some(components)
    }
  }
}
