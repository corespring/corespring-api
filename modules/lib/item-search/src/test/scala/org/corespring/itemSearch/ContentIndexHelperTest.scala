package org.corespring.itemSearch

import java.net.URL

import org.bson.types.ObjectId
import org.corespring.elasticsearch.Index
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.JsValue
import play.api.libs.json.Json._
import play.api.libs.ws.{ Response, WS }

import scala.collection.mutable
import scala.concurrent.{ ExecutionContext, Future }
import scalaz.Success

class ContentIndexHelperTest extends Specification with Mockito {

  trait scope extends Scope {

    val oid = ObjectId.get

    lazy val index = {
      val m = mock[Index]
      m
    }

    lazy val holders: mutable.Map[String, WS.WSRequestHolder] = mutable.Map()

    lazy val elasticSearchExecutionContext = ElasticSearchExecutionContext(ExecutionContext.global)

    def mockResponse(body: String) = {
      val m = mock[Response]
      m.body.returns(body)
      m
    }

    def mockHolder(body: String) = {
      val m = mock[WS.WSRequestHolder]
      m.post(any[JsValue]).returns {
        Future.successful(mockResponse(body))
      }
      m
    }

    lazy val url = {
      val m = mock[AuthenticatedUrl]
      m.authed(any[String])(any[URL], any[ExecutionContext]) answers { (args, _) =>
        {
          val (url, _, _) = (args.asInstanceOf[Array[Any]])
          val holder = mockHolder("{}")
          holders.put(url, holder)
          holder
        }
      }
    }

    val helper = new ContentIndexHelper(
      index,
      elasticSearchExecutionContext,
      url)
  }

  "addlatest" should {

    "return a successful result" in new scope {
      val result = helper.addLatest(oid, 0, obj())
      result must equalTo(Success("??")).await
    }
  }

}
