package org.corespring.v2.player

import org.corespring.container.client.controllers.DataQuery
import org.corespring.it.IntegrationSpecification
import org.specs2.specification.Scope
import play.api.libs.json.{ JsArray, JsObject, JsValue }
import play.api.mvc.SimpleResult
import play.api.test.FakeRequest
import play.api.{ GlobalSettings, Play }

import scala.concurrent.Future

class DataQueryTest extends IntegrationSpecification {

  class listScope extends Scope {
    protected def global: GlobalSettings = Play.current.global

    def listResult(topic: String): Future[SimpleResult] = {
      val dataQuery = global.getControllerInstance(classOf[DataQuery])
      val list = dataQuery.list(topic)
      list(FakeRequest("", ""))
    }
  }

  "data query" should {

    "work" in new listScope {
      forall(
        Seq(
          "licenseTypes",
          "mediaType",
          "depthOfKnowledge",
          "keySkills",
          "priorUses",
          "reviewsPassed",
          "credentials",
          "standardsTree",
          "bloomsTaxonomy",
          "subjects.primary",
          "subjects.related",
          "standards")) { (topic: String) =>
          val r = listResult(topic)
          contentAsJson(r) match {
            case obj: JsObject => {
              failure((obj \ "error").as[String])
            }
            case arr: JsArray => {
              (arr.as[Seq[JsValue]].length > 0) === true
            }
            case _ => failure("??")
          }
        }

    }
  }
}
