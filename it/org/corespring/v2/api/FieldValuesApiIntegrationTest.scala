package org.corespring.v2.api

import global.Global
import org.bson.types.ObjectId
import org.corespring.it.IntegrationSpecification
import org.corespring.models.{ Subject, Standard }
import org.specs2.mutable.{ Before, After }
import org.specs2.specification.Scope
import play.api.libs.json.{ Json, JsValue }
import play.api.mvc.Call
import play.api.test.FakeRequest

class FieldValuesApiIntegrationTest extends IntegrationSpecification {

  trait scope[T] extends Scope with Before {

    def data: Seq[T] = Seq.empty
    def query: Option[JsValue]
    def call: Call
    def insert(t: T): Option[ObjectId]
    lazy val req = FakeRequest(call.method, call.url)
    lazy val result = {
      data.map(insert(_))
      route(req).get
    }

    lazy val queryString = query.map(Json.stringify(_))

    override def before: Any = {
      removeData()
    }
  }

  trait standard extends scope[Standard] {
    lazy val call = org.corespring.v2.api.routes.FieldValuesApi.standard(queryString)
    override def insert(t: Standard): Option[ObjectId] = Global.main.standardService.insert(t)
  }

  trait subject extends scope[Subject] {
    lazy val call = org.corespring.v2.api.routes.FieldValuesApi.subject(queryString)
    override def insert(t: Subject): Option[ObjectId] = Global.main.subjectService.insert(t)
  }

  "standard" should {

    "fail if the json can't be read as a query" in new standard {
      override lazy val query = Some(Json.obj())
      println(contentAsString(result))
      status(result) === BAD_REQUEST
    }

    "call list if there is no query" in new standard {
      override lazy val data = Seq(
        Standard(subject = Some("apple")),
        Standard(subject = Some("banana")))

      override lazy val query = None
      contentAsJson(result).as[Seq[JsValue]].length === 2
    }

    "find standards with an 'a' in them" in new standard {
      override lazy val data = Seq(
        Standard(subject = Some("apple")),
        Standard(subject = Some("banana")),
        Standard(subject = Some("plum")))

      override lazy val query = Some(Json.obj("term" -> "a"))
      contentAsJson(result).as[Seq[JsValue]].length === 2
    }
  }

  "subject" should {

    "fail if the json can't be read as a query" in new subject {
      override lazy val query = Some(Json.obj())
      println(contentAsString(result))
      status(result) === BAD_REQUEST
    }

    "call list if there is no query" in new subject {
      override lazy val data = Seq(
        Subject("subject", category = Some("apple")),
        Subject("subject", category = Some("banana")))

      override lazy val query = None
      contentAsJson(result).as[Seq[JsValue]].length === 2
    }

    "find standards with an 'a' in them" in new subject {
      override lazy val data = Seq(
        Subject("subject", category = Some("apple")),
        Subject("subject", category = Some("banana")),
        Subject("subject", category = Some("plum")))

      override lazy val query = Some(Json.obj("term" -> "a"))
      contentAsJson(result).as[Seq[JsValue]].length === 2
    }
  }
}
