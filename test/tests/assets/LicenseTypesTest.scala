package tests.assets

import org.corespring.test.BaseTest
import play.api.test.FakeRequest
import play.api.test.Helpers._
import scala.Some

class LicenseTypesTest extends BaseTest{
  def assertLicenseType(name:String): org.specs2.execute.Result = {
    val fakeRequest = FakeRequest(GET, "/assets/images/licenseTypes/"+name)
    val Some(result) = route(fakeRequest)
    status(result) === OK
  }

  "each license type" should {
    "return 200" in {
      assertLicenseType("CC-BY-NC-SA.png")
      assertLicenseType("CC-BY-NC.png")
      assertLicenseType("CC-BY-ND.png")
      assertLicenseType("CC-BY-SA.png")
      assertLicenseType("CC-BY.png")
    }
  }
}
