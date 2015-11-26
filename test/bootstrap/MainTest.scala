package bootstrap

import play.api.test.{ PlaySpecification, FakeApplication }

class MainTest extends PlaySpecification {

  "resolveDomain" should {

    def mkConfig(domain: String, queryParam: Boolean) = Map(
      "container" -> Map(
        "cdn" -> Map(
          "domain" -> domain,
          "add-version-as-query-param" -> queryParam)))

    "return the path with the cdn prefixed if the cdn is configured" in {
      val config = mkConfig("//blah.com", false)
      running(FakeApplication(additionalConfiguration = config)) {
        Main.resolveDomain("hi") must_== "//blah.com/hi"
      }
    }
  }
}
