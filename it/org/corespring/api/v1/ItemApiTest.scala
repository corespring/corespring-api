package org.corespring.api.v1

import org.corespring.it.ITSpec
import org.corespring.test.helpers.FixtureData
import play.api.libs.json.{JsSuccess, Json, Reads, JsValue}
import play.api.mvc.SimpleResult
import play.api.test.FakeRequest
import scala.Some
import scala.concurrent.Future
import org.specs2.specification.AroundExample
import org.specs2.execute.{Result, AsResult}
import org.corespring.test.helpers.models._
import play.api.libs.json.JsSuccess
import scala.Some
import play.api.mvc.SimpleResult
import scala.collection.parallel.mutable

class ItemApiTest extends ITSpec{

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

  "list items in a collection" in new FixtureData {
    println(s"[Test] collection: $collectionId, token: $accessToken")
    //TODO: Don't use magic strings for the routes - call the controller directly
    val fakeRequest = FakeRequest("", s"?access_token=$accessToken")
    val result = ItemApi.listWithColl(collectionId, None, None, "false", 0, 50, None)(fakeRequest)
    println(s" content: ---->  ${contentAsString(result)}")
    assertResult(result)
    val items = parsed[List[JsValue]](result)
    items.size must beEqualTo(itemIds.length)
  }
}
