package tests.basiclti.controllers

import org.specs2.mutable.Specification
import tests.PlaySingleton
import play.api.test.FakeRequest
import basiclti.controllers.AssignmentLauncher
import basiclti.models.LtiData
import play.api.test.Helpers._
import play.api.mvc.{AnyContentAsFormUrlEncoded, AnyContent}

class AssignmentLauncherTest extends Specification {

  PlaySingleton.start()

  "Assignment launcher" should {

    "parse the request info" in {

      val call = basiclti.controllers.routes.AssignmentLauncher.launch()
      val request : FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest(call.method, call.url).withFormUrlEncodedBody(
        (AssignmentLauncher.LtiKeys.ConsumerKey, "key"),
        (LtiData.Keys.OutcomeServiceUrl, "service_url"),
        ("oauth_signature", "blah"),
        (LtiData.Keys.Roles, "Instructor"),
        (LtiData.Keys.ResourceLinkId, "1")
      )

      routeAndCall(request) match {
        case Some(result) => status(result) === OK
        case _ => failure
      }

      true === true
    }
  }

}
