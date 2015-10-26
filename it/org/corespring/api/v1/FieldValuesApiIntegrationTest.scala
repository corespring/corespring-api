package org.corespring.api.v1

import org.bson.types.ObjectId
import org.corespring.it.IntegrationSpecification
import org.corespring.it.scopes.{ TokenRequestBuilder, orgWithAccessToken }
import org.corespring.models.{ Subject, Standard }
import org.specs2.specification.Scope
import play.api.libs.json.{ JsObject, JsValue, Json }
import play.api.mvc.Call

class FieldValuesApiIntegrationTest extends IntegrationSpecification {

  trait scope[T] extends Scope with orgWithAccessToken with TokenRequestBuilder {

    def data: Seq[T] = Seq.empty
    def call: Call
    def insert(d: T): Option[ObjectId]
    def query: Option[JsObject]
    lazy val queryString = query.map(Json.stringify(_))
    lazy val req = makeRequest(call)

    lazy val result = {

      logger.debug(s"seed the standards")
      data.map { s =>
        insert(s)
      }

      logger.debug("now route the request")
      route(req).get
    }

    def regex(s: String) = Json.obj("$regex" -> s"\\\\b$s", "options" -> "i")
  }

  trait standard extends scope[Standard] {
    override def call: Call = org.corespring.api.v1.routes.FieldValuesApi.standard(queryString)

    override def insert(d: Standard): Option[ObjectId] = {
      bootstrap.Main.standardService.insert(d)
    }
  }

  trait subject extends scope[Subject] {
    override def call: Call = org.corespring.api.v1.routes.FieldValuesApi.subject(queryString)

    override def insert(d: Subject): Option[ObjectId] = {
      bootstrap.Main.subjectService.insert(d)
    }
  }

  "standard" should {

    "fail if the json doesn't contain an $or operator" in new standard {
      override lazy val query = Some(Json.obj())
      status(result) === BAD_REQUEST
    }

    "fail if the $or array is empty" in new standard {
      override lazy val query = Some(Json.obj("$or" -> Json.arr()))
      status(result) === BAD_REQUEST
    }

    "return standards with a subject that has an 'a' in it" in new standard {

      override lazy val data = Seq(
        Standard(subject = Some("apple")),
        Standard(subject = Some("banana")),
        Standard(subject = Some("plum")))

      override lazy val query = Some(Json.obj("$or" -> Json.arr(
        Json.obj("subject" -> regex("a")))))

      status(result) === OK
      contentAsJson(result).as[Seq[JsValue]].length === 2
    }
  }

  "subject" should {

    "fail if the json doesn't contain an $or operator" in new subject {
      override lazy val query = Some(Json.obj())
      status(result) === BAD_REQUEST
    }

    "fail if the $or array is empty" in new subject {
      override lazy val query = Some(Json.obj("$or" -> Json.arr()))
      status(result) === BAD_REQUEST
    }

    "return subjects with a category that has an 'a' in it" in new subject {

      override lazy val data = Seq(
        Subject(subject = "subject", category = Some("apple")),
        Subject(subject = "subject", category = Some("banana")),
        Subject(subject = "subject", category = Some("plum")))

      override lazy val query = Some(Json.obj("$or" -> Json.arr(
        Json.obj("subject" -> regex("a")))))

      status(result) === OK
      contentAsJson(result).as[Seq[JsValue]].length === 2
    }

  }
}
