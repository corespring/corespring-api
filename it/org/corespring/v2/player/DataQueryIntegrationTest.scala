package org.corespring.v2.player

import global.Global
import org.corespring.it.IntegrationSpecification
import org.corespring.models.{ Standard, Subject }
import org.corespring.models.item.{ FieldValue, ListKeyValue, StringKeyValue }
import org.specs2.mutable.After
import play.api.libs.json.{ Json, JsArray, JsObject, JsValue }
import play.api.mvc.SimpleResult
import play.api.test.FakeRequest

import scala.concurrent.Future

import org.corespring.container.client.controllers.routes.DataQuery

class DataQueryIntegrationTest extends IntegrationSpecification {

  trait listScope extends After {

    lazy val fieldValueService = Global.main.fieldValueService
    lazy val subjectService = Global.main.subjectService

    lazy val standardService = Global.main.standardService

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

    "list a json array standards query" in new listScope {
      val call = DataQuery.list("standards", Some(s"""{"dotNotation" : "DOT.NOTATION"}"""))
      val result = route(FakeRequest(call.method, call.url)).get
      val jsArray = contentAsJson(result).as[JsArray]
      (jsArray(0) \ "dotNotation").as[String] must_== "DOT.NOTATION"
    }

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
