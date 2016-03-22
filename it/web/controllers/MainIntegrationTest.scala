package web.controllers

import org.corespring.it.IntegrationSpecification
import org.corespring.it.helpers.{ ApiClientHelper, SecureSocialHelper }
import org.corespring.it.scopes.{ SessionRequestBuilder, userAndItem }
import play.api.libs.json.Json
import play.api.test.FakeRequest

class MainIntegrationTest extends IntegrationSpecification {

  "version" should {

    val call = web.controllers.routes.Main.version
    val req = FakeRequest(call.method, call.url)

    "return OK" in {
      val result = route(req).get
      status(result) must_== OK
    }

    "return version info" in {
      val result = route(req).get
      contentAsJson(result) must_== main.buildInfo.json.deepMerge(Json.obj("container" -> main.versionInfo.json))
    }

  }

  "sampleLaunchCode" should {

    "generated sample code contains apiclientid and itemid" in new userAndItem with SessionRequestBuilder with SecureSocialHelper {
      val client = ApiClientHelper.create(orgId)
      val call = web.controllers.routes.Main.sampleLaunchCode(itemId.toString)

      val result = {
        val request = makeRequest(web.controllers.routes.Main.sampleLaunchCode(itemId.toString))
        route(request)(writeable)
      }

      result.map { r =>
        val content = contentAsString(r)
        status(r) must_== OK
        content must contain(client.clientId.toString)
        content must contain(itemId.toString)
      }

      ApiClientHelper.delete(client)
    }
  }
}
