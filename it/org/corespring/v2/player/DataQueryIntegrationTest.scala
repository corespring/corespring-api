package org.corespring.v2.player

import org.corespring.it.IntegrationSpecification
import org.corespring.models.{ Standard, Subject }
import org.corespring.models.item.{ FieldValue, ListKeyValue, StringKeyValue }
import org.specs2.mutable.After
import play.api.libs.json.{ JsArray, JsObject, JsValue }
import play.api.mvc.SimpleResult
import play.api.test.FakeRequest

import scala.concurrent.Future

class DataQueryIntegrationTest extends IntegrationSpecification {

  trait listScope extends After {

    lazy val fieldValueService = bootstrap.Main.fieldValueService
    lazy val subjectService = bootstrap.Main.subjectService

    lazy val standardService = bootstrap.Main.standardService

    val dummy = StringKeyValue("dummy", "dummy")
    val dummyList = ListKeyValue("dummy", Seq.empty)
    val fieldValue = FieldValue(
      gradeLevels = Seq(dummy),
      reviewsPassed = Seq(dummy),
      mediaType = Seq(dummy),
      keySkills = Seq(dummyList),
      itemTypes = Seq(dummyList),
      licenseTypes = Seq(dummy),
      priorUses = Seq(dummy),
      depthOfKnowledge = Seq(dummy),
      credentials = Seq(dummy),
      bloomsTaxonomy = Seq(dummy))
    val id = fieldValueService.insert(fieldValue).toOption

    val subjectId = subjectService.insert(Subject("Subject", Some("Category")))
    val standardId = standardService.insert(Standard(Some("DOT.NOTATION")))

    def listResult(topic: String): Future[SimpleResult] = {
      import org.corespring.container.client.controllers.routes.DataQuery
      val call = DataQuery.list(topic)
      route(FakeRequest(call.method, call.url)).getOrElse {
        throw new RuntimeException("data-query list failed")
      }
    }

    override def after: Any = {
      fieldValueService.delete(id.get)
      standardService.delete(standardId.get)
      subjectService.delete(subjectId.get)
    }
  }

  "data query" should {

    "list should return a list for every topic" in new listScope {
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
            case obj: JsObject => ko((obj \ "error").as[String])
            case arr: JsArray => arr.as[Seq[JsValue]].nonEmpty === true
            case _ => ko("??")
          }
        }

    }
  }
}
