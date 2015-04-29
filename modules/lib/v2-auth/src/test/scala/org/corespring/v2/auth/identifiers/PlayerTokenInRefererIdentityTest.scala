package org.corespring.v2.auth.identifiers

import org.specs2.mutable.Specification
import play.api.test.FakeRequest

class PlayerTokenInRefererIdentityTest extends Specification {

  import PlayerTokenInRefererIdentity._

  "RequestHeaderWithRefererQueryString" should {

    val queryMap = Map("this" -> Seq("is"), "my" -> Seq("query"))
    val queryString = {
      val enc = (p: String) => java.net.URLEncoder.encode(p, "utf-8")
      queryMap.map{ case(k, v) => s"${enc(k)}=${enc(v.head)}"}.mkString("&")
    }
    val url = "http://localhost:9000"
    val referer = s"$url?$queryString"
    val request = FakeRequest().withHeaders("Referer" -> referer)

    "provide query string from header" in {
      request.getRefererQueryString must be equalTo(queryMap)
    }

  }

}
