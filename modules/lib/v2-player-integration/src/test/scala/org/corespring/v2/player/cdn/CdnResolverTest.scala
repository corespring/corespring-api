package org.corespring.v2.player.cdn

import org.specs2.mutable.Specification

class CdnResolverTest extends Specification {

  "resolveDomain" should {
    "ignore domains that do not start with two slashes" in {
      val cdnResolver = new CdnResolver(Some("blah.com"), None)
      cdnResolver.resolveDomain("mypath") === "mypath"
    }

    "accept domains that do start with two slashes" in {
      val cdnResolver = new CdnResolver(Some("//blah.com"), None)
      cdnResolver.resolveDomain("") === "//blah.com/"
    }

    "add slash if path doesn't start with a slash" in {
      val cdnResolver = new CdnResolver(Some("//blah.com"), None)
      cdnResolver.resolveDomain("path") === "//blah.com/path"
    }

    "do not add slash if path does start with a slash" in {
      val cdnResolver = new CdnResolver(Some("//blah.com"), None)
      cdnResolver.resolveDomain("/path") === "//blah.com/path"
    }

    "add version if defined" in {
      val cdnResolver = new CdnResolver(Some("//blah.com"), Some("v1234"))
      cdnResolver.resolveDomain("/path") === "//blah.com/path?version=v1234"
    }

    "should use ampersand if path has query already" in {
      val cdnResolver = new CdnResolver(Some("//blah.com"), Some("v1234"))
      cdnResolver.resolveDomain("/path?query") === "//blah.com/path?query&version=v1234"
    }

  }
}
