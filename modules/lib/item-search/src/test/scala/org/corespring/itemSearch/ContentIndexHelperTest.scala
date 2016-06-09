package org.corespring.itemSearch

import java.net.URL

import org.bson.types.ObjectId
import org.corespring.elasticsearch.Index
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import org.specs2.time.NoTimeConversions
import play.api.http.{ ContentTypeOf, Writeable }
import play.api.libs.json.JsValue
import play.api.libs.json.Json._
import play.api.libs.ws.WS.WSRequestHolder
import play.api.libs.ws.{ Response, WS, WSRequestHolderOps }

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext, Future }
import scalaz.Success

class ContentIndexHelperTest extends Specification with Mockito with NoTimeConversions {

  trait scope extends Scope {

    implicit def mockOps(rh: WSRequestHolder): WSRequestHolderOps = {
      val m = mock[WSRequestHolderOps]

      m.delete(any[JsValue])(any[Writeable[JsValue]], any[ContentTypeOf[JsValue]]) returns {
        Future.successful(mockResponse("{}"))
      }
      m
    }

    val oid = ObjectId.get

    lazy val mockSuccess = """{"success": true}"""

    protected def await[A](f: Future[A]) = Await.result(f, 1.second)

    lazy val index = {
      val m = mock[Index]
      m.add(any[String], any[String]) returns {
        Future.successful(Success(mockSuccess))
      }
      m
    }

    lazy val deleteHolder = mockHolder("""{"acknowledged" : true}""")
    lazy val updateHolder = mockHolder("""{"acknowledged" : true}""")

    lazy val elasticSearchExecutionContext = ElasticSearchExecutionContext(ExecutionContext.global)

    def mockResponse(body: String) = {
      val m = mock[Response]
      m.body.returns(body)
      m
    }

    def mockHolder(body: String) = {
      val m = mock[WS.WSRequestHolder]
      m.post(any[JsValue])(any[Writeable[JsValue]], any[ContentTypeOf[JsValue]]).returns {
        Future.successful(mockResponse(body))
      }
      //Ahah - how to mock the implicit class
      m.delete(any[JsValue])(any[Writeable[JsValue]], any[ContentTypeOf[JsValue]]).returns {
        Future.successful(mockResponse(body))
      }
      m
    }

    lazy val auth = {
      val m = mock[AuthenticatedUrl]
      m.authed(any[String])(any[URL], any[ExecutionContext]) answers { (args, _) =>
        {
          val arr = (args.asInstanceOf[Array[Any]])
          val url = arr(0).asInstanceOf[String]

          url match {
            case "/content/content/_query" => deleteHolder
            case _ => updateHolder
          }
        }
      }
    }

    val url = new URL("http://localhost:4040")

    val helper = new ContentIndexHelper(
      index,
      elasticSearchExecutionContext,
      auth,
      url)

  }

  "addlatest" should {

    "return a successful result" in new scope {
      lazy val result = helper.addLatest(oid, 0, obj())
      result must equalTo(Success(mockSuccess)).await
    }

    "does not call delete if version is 0" in new scope {
      await(helper.addLatest(oid, 0, obj()))
      there was no(deleteHolder).delete(any[JsValue])(any[Writeable[JsValue]], any[ContentTypeOf[JsValue]])
    }.pendingUntilFixed("how to verify implicit mocks")
  }

}
