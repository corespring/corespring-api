package org.corespring.test

import org.specs2.mutable.SpecificationLike
import play.api.libs.json.{JsSuccess, Json, Reads}
import play.api.mvc.SimpleResult
import play.api.test.Helpers._
import scala.concurrent.Future

trait Assertions extends SpecificationLike{

  def assertResult(result: Future[SimpleResult],
                   expectedStatus: Int = OK,
                   expectedCharset: Option[String] = Some("utf-8"),
                   expectedContentType: Option[String] = Some("application/json")): org.specs2.execute.Result = {
    status(result) === expectedStatus
    charset(result) === expectedCharset
    contentType(result) === expectedContentType
  }

  def parsed[A](result: Future[SimpleResult])(implicit reads: Reads[A]) = Json.fromJson[A](Json.parse(contentAsString(result))) match {
    case JsSuccess(data, _) => data
    case _ => throw new RuntimeException("Couldn't parse json")
  }
}
