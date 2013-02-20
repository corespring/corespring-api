package tests.publicsite.controllers

import tests.PlaySingleton
import org.specs2.mutable.Specification
import play.api.test.Helpers._
import models.item._
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.mvc.Result
import play.api.mvc.AnyContent
import scala.Some

class ExampleContentTest extends Specification {

  PlaySingleton.start()

  "ExampleContent" should {

    "return the items" in {

      val call = publicsite.controllers.routes.ExampleContent.items(None)

      val request = FakeRequest(call.method, call.url)

      routeAndCall(request) match {

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
