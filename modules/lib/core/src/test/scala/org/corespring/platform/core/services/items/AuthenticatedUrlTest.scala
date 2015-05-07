package org.corespring.platform.core.services.items

import java.net.URL

import org.apache.commons.codec.binary.Base64._
import org.corespring.platform.core.services.item.AuthenticatedUrl
import org.corespring.test.PlaySingleton
import org.specs2.mutable.Specification
import play.api.libs.ws.WS.WSRequestHolder

import scala.concurrent.ExecutionContext

class AuthenticatedUrlTest extends Specification with AuthenticatedUrl {

  PlaySingleton.start()

  import ExecutionContext.Implicits.global
  import play.api.Play.current

  def authorizationHeader(request: WSRequestHolder): Option[(String, Seq[String])] =
    request.headers.find{ case(key, _) => key == "Authorization"}

  val route = "/search"

  "authed" should {

    "no authentication in url" should {

      implicit val url = new URL("http://localhost:2900")
      val result = authed(route)

      "not contain authorization header" in {
        authorizationHeader(result) must beEmpty
      }

    }

    "authentication in url" should {
      val username = "benburton"
      val password = "greatpassword"
      implicit val url = new URL(s"http://$username:$password@localhost:2900")
      val result = authed(route)

      "contain authorization header" in {
        authorizationHeader(result) must not beEmpty
      }

      "use basic http authentication" in {
        authorizationHeader(result) match {
          case Some((_, Seq(httpAuth))) =>
            httpAuth === s"Basic ${new String(encodeBase64(s"$username:$password".getBytes))}"
          case _ => failure("did not contain authroization header")
        }
      }

    }

    "port in base url" should {

      val port = "9200"
      implicit val url = new URL(s"http://localhost:$port")
      val result = authed(route)

      "include port in request base url" in {
        result.url must beEqualTo(s"$url$route")
      }

    }

  }

}
