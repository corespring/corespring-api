package tests.publicsite.controllers

import org.corespring.platform.core.models.item._
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import scala.Some
import org.corespring.platform.core.models.item.Item
import org.corespring.test.BaseTest

class ExampleContentTest extends BaseTest{

  "ExampleContent" should {

    "return the items" in {

      val call = publicsite.controllers.routes.ExampleContent.items(None)

      val request = FakeRequest(call.method, call.url)

      route(request) match {

        case Some(result) => {
          val json = Json.parse(contentAsString(result))

          val items: Seq[Item] = json.as[Seq[Item]]
          items.head.taskInfo.get.title !== None
          items.head.taskInfo.get.itemType !== None
          items.head.taskInfo.get.subjects !== None

        }
        case _ => failure("no result")

      }
    }
  }


}
