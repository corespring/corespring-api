package org.corespring.v2.auth.models

import org.specs2.mutable.Specification
import play.api.libs.json.{JsValue, Json}

class IdentityJsonTest extends Specification with MockFactory{


  "IdentityJson.apply" should {

    "create json with apiClient" in {
      val o = mockOrg

      val json = IdentityJson(OrgAndOpts(o, PlayerAccessSettings.ANYTHING, AuthMode.AccessToken, Some("1")))

      json === Json.parse(
        s"""{
           |  "orgId" : "${o.id}",
           |  "authMode" : "${AuthMode.AccessToken.toString}",
           |  "apiClient" : "1"
           |}""".stripMargin)
    }

    "create json with apiClient set to 'unknown'" in {
      val o = mockOrg

      val json = IdentityJson(OrgAndOpts(o, PlayerAccessSettings.ANYTHING, AuthMode.AccessToken, None))

      json === Json.parse(
        s"""{
           |  "orgId" : "${o.id}",
           |  "authMode" : "${AuthMode.AccessToken.toString}",
           |  "apiClient" : "unknown"
           |}""".stripMargin)
    }
  }
}
