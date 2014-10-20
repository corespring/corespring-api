package org.corespring.v2.player

import com.typesafe.config.ConfigFactory
import org.specs2.mutable.Specification
import play.api.Configuration
import org.specs2.specification._


class CDNResolverTest extends Specification {

  private class TestContext(configJson: String) extends Before {
    def before {}
    val config = ConfigFactory.parseString(configJson)
    val configuration = new Configuration(config)
    val cdnResolver = new CDNResolver(configuration, "v1234")
  }


  "resolveDomain" should {
    "ignore domains that do not start with two slashes" in new TestContext("{cdn:{domain:some-domain}}"){
      cdnResolver.resolveDomain("mypath") === "mypath"
    }

    "accept domains that do start with two slashes" in new TestContext("""{cdn:{domain:"//some-domain"}}"""){
      cdnResolver.resolveDomain("") === "//some-domain/"
    }

    "add slash if path doesn't start with a slash" in new TestContext("""{cdn:{domain:"//some-domain"}}"""){
      cdnResolver.resolveDomain("path") === "//some-domain/path"
    }

    "do not add slash if path does start with a slash" in new TestContext("""{cdn:{domain:"//some-domain"}}"""){
      cdnResolver.resolveDomain("/path") === "//some-domain/path"
    }

    "add version if addVersionConfig is true" in new TestContext("""{cdn:{domain:"//some-domain", "add-version-as-query-param": true}}"""){
      cdnResolver.resolveDomain("/path") === "//some-domain/path?version=v1234"
    }

    "should use ampersand if path has query already" in new TestContext("""{cdn:{domain:"//some-domain", "add-version-as-query-param": true}}"""){
      cdnResolver.resolveDomain("/path?query") === "//some-domain/path?query&version=v1234"
    }

  }
}
