package conf

import org.specs2.mutable.Specification
import play.api.test.Helpers._
import play.api.test._
import org.corespring.test.PlaySingleton

class RoutesTest extends Specification {

  PlaySingleton.start()

  "/player/encrypt-options" should {
    val Some(result) = route(FakeRequest(POST, "/player/encrypt-options"))

    "be a 303 SEE_OTHER" in {
      status(result) must be equalTo SEE_OTHER
    }

    "redirect to /player/token" in {
      header("Location", result) must be equalTo Some("/player/token")
    }

  }

  "api/v1/player/token" should {
    val Some(result) = route(FakeRequest(POST, "/api/v1/player/token"))

    "be a 303 SEE_OTHER" in {
      status(result) must be equalTo SEE_OTHER
    }

    "redirect to /player/token" in {
      header("Location", result) must be equalTo Some("/player/token")
    }

  }

}
