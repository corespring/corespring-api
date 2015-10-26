package org.corespring.web.publicsite.controllers

import org.corespring.models.item._
import org.corespring.test.BaseTest
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._

class ExampleContentTest extends BaseTest {

  "ExampleContent" should {

    "return the items" in {

      val request = FakeRequest("", "")

      val result = ExampleContent.items(None)(request)

      val json = Json.parse(contentAsString(result))
      val items: Seq[Item] = json.as[Seq[Item]]
      items.head.taskInfo.get.title !== None
      items.head.taskInfo.get.itemType !== None
      items.head.taskInfo.get.subjects !== None

    }
  }

}
