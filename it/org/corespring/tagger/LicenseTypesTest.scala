package org.corespring.tagger

import org.corespring.it.IntegrationSpecification
import org.specs2.execute.Result
import play.api.test.FakeRequest

class LicenseTypesTest extends IntegrationSpecification {
  def assertLicenseType(name: String): Result = {
    val fakeRequest = FakeRequest(GET, "/assets/images/licenseTypes/" + name)
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