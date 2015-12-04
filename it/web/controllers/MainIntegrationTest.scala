package web.controllers

import org.corespring.it.IntegrationSpecification
import org.corespring.web.common.views.helpers.BuildInfo
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
      contentAsJson(result) must_== BuildInfo.json.deepMerge(Json.obj("container" -> bootstrap.Main.containerVersion.json))
    }

  }

}
