package bootstrap

import filters.CacheFilter
import play.api.test.{ PlaySpecification, FakeApplication }

class MainTest extends PlaySpecification {

  def mkConfig(domain: String, queryParam: Boolean) = Map(
    "container" -> Map(
      "cdn" -> Map(
        "domain" -> domain,
        "add-version-as-query-param" -> queryParam)))

  val config = mkConfig("//blah.com", false)

  running(FakeApplication(additionalConfiguration = config)) {
    "Main" should {
      "use new CacheFilter" in {
        Main.componentSetFilter must haveInterface[CacheFilter]
      }
    }

    "resolveDomain" should {

      "return the path with the cdn prefixed if the cdn is configured" in {
        Main.resolveDomain("hi") must_== "//blah.com/hi"
      }
    }
  }
}
