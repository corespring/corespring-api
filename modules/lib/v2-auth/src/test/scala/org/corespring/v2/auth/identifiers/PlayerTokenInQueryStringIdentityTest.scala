package org.corespring.v2.auth.identifiers

import org.bson.types.ObjectId
import org.corespring.platform.core.models.Organization
import org.corespring.v2.auth.models.{ OrgAndOpts, PlayerAccessSettings }
import org.corespring.v2.auth.services.OrgService
import org.corespring.v2.errors.Errors.{ invalidQueryStringParameter, noApiClientAndPlayerTokenInQueryString }
import org.corespring.v2.warnings.Warnings.deprecatedQueryStringParameter
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import play.api.libs.json.Json
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest

import scalaz.{ Failure, Success }

class PlayerTokenInQueryStringIdentityTest extends Specification with Mockito {

  val org = {
    val m = mock[Organization]
    m.id returns ObjectId.get
    m
  }

  val identifier = new PlayerTokenInQueryStringIdentity {
    override def clientIdToOrg(apiClientId: String): Option[Organization] = Some(org)

    override def decrypt(encrypted: String, orgId: ObjectId, header: RequestHeader): Option[String] = Some(encrypted)

    override def orgService: OrgService = {
      val m = mock[OrgService]
      m.defaultCollection(any[Organization]) returns Some(ObjectId.get)
      m.org(any[ObjectId]) returns Some(org)
      m
    }
  }

  "building identity" should {

    s"return a bad param name error" in {
      identifier.headerToOrg(FakeRequest("GET", "?apiClientId=blah")) must_== Failure(invalidQueryStringParameter("apiClientId", PlayerTokenInQueryStringIdentity.Keys.apiClient))
    }

    "return no apiClientAndPlayerToken error" in {
      identifier(FakeRequest("GET", "bad")) must_== Failure(noApiClientAndPlayerTokenInQueryString(FakeRequest("GET", "bad")))
    }

    "return a warning if 'options' is used as a queryString param" in {
      import _root_.org.corespring.v2.auth.identifiers.PlayerTokenInQueryStringIdentity.Keys._
      val jsonSettings = Json.stringify(Json.toJson(PlayerAccessSettings.ANYTHING))
      val request = FakeRequest("GET", s"""?$apiClient=1&$options=${jsonSettings}""")
      val result = identifier.apply(request)

      result match {
        case Success(OrgAndOpts(_, _, _, warnings)) => {
          warnings(0) === deprecatedQueryStringParameter(options, playerToken)
        }
        case _ => failure("didn't find warning")
      }
    }

    "return success with no warnings" in {
      import _root_.org.corespring.v2.auth.identifiers.PlayerTokenInQueryStringIdentity.Keys._
      val jsonSettings = Json.stringify(Json.toJson(PlayerAccessSettings.ANYTHING))
      val request = FakeRequest("GET", s"""?$apiClient=1&$playerToken=${jsonSettings}""")
      val result = identifier.apply(request)

      result match {
        case Success(OrgAndOpts(org, _, _, warnings)) => {
          warnings.length === 0
          org === this.org
        }
        case _ => failure("didn't find warning")
      }
    }
  }
}
