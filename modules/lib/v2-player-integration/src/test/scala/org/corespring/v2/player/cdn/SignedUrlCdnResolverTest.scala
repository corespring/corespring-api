package org.corespring.v2.player.cdn

import java.util.Date

import org.joda.time.DateTime
import org.specs2.matcher.{Matcher, Expectable}
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

class SignedUrlCdnResolverTest extends Specification with Mockito {

  class TestUrlSigner extends CdnUrlSigner("",""){
    override def signUrl(url: String, validUntil: Date): String = url
  }

  trait scope extends Scope {
    val urlSigner = spy(new TestUrlSigner)

    def mkResolver(domain: Option[String], version: Option[String] = None, hours: Int = 24): CdnResolver = {
      new SignedUrlCdnResolver(domain, version, urlSigner, hours)
    }

    def beCloseInTimeTo(date: Date, timeDiff: Int = 500) = new Matcher[Date] {
      def apply[D <: Date](e: Expectable[D]) =
        result((e.value.getTime - date.getTime) < timeDiff,
          "Dates are nearly at the same time",
          "Dates are different",
          e)
    }


  }

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

    "call signUrl" in new scope {
      val cdnResolver = mkResolver(Some("//blah.com"), Some("v1234"))
      cdnResolver.resolveDomain("/path?query")
      there was one(urlSigner).signUrl(any[String], any[Date])
    }

    "pass the correct url to signUrl" in new scope {
      val cdnResolver = mkResolver(Some("//blah.com"), Some("v1234"))
      cdnResolver.resolveDomain("/path?query")
      there was one(urlSigner).signUrl(===("//blah.com/path?query&version=v1234"), any[Date])
    }

    "pass the correct validUntil date to signUrl" in new scope {
      val hours = 12
      val expectedDate = DateTime.now().plusHours(hours).toDate
      val cdnResolver = mkResolver(Some("//blah.com"), Some("v1234"), hours)
      cdnResolver.resolveDomain("/path?query")
      there was one(urlSigner).signUrl(any[String], beCloseInTimeTo(expectedDate))
    }

  }
}
