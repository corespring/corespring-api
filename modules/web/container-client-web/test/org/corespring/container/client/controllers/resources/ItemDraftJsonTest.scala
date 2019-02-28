package org.corespring.container.client.controllers.resources

import org.corespring.container.client.controllers.helpers.PlayerXhtml
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import play.api.libs.json.Json

class ItemDraftJsonTest extends Specification with Mockito {

  val json = Json.obj(
    "_id" -> Json.obj("$oid" -> "1"),
    "xhtml" -> "<p>hello</p>")

  val playerXhtml = {
    val m = mock[PlayerXhtml]

    m.processXhtml(any[String]) answers {
      (s: Any) => s.asInstanceOf[String]
    }

    m
  }

  val itemJson = ItemJson(playerXhtml, json)

  "ItemJson" should {

    "add itemId" in {
      (itemJson \ "itemId").as[String] === "1"
    }

    "call playerXhtml.processXhtml" in {
      there was one(playerXhtml).processXhtml("<p>hello</p>")
    }

    "not set itemId if _id.$oid is missing" in {
      val out = ItemJson(playerXhtml, Json.obj("xhtml" -> "x"))
      (out \ "itemId").asOpt[String] === None
    }
  }
}
