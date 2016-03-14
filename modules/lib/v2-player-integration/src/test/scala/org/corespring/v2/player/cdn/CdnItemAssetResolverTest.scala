package org.corespring.v2.player.cdn

import java.util.Date

import org.specs2.matcher.{ Expectable, Matcher }
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

class CdnItemAssetResolverTest extends Specification with Mockito {

  "CdnItemAssetResolver" should {

    trait withoutSigning extends Scope {

      val cdnResolver = new CdnResolver(Some("//blah"), None)

      val sut = new CdnItemAssetResolver(cdnResolver)
    }

    "resolve" should {

      "return the file" in new withoutSigning {
        sut.resolve("123456789012345678901234:0")("FigurePattern.png") === "//blah/player/item/123456789012345678901234:0/FigurePattern.png"
      }
    }

    trait withSigning extends Scope {

      class TestUrlSigner extends CdnUrlSigner("", "") {
        override def signUrl(url: String, validUntil: Date): String = url
      }

      val urlSigner = spy(new TestUrlSigner)

      def mkSigningResolver(domain: Option[String], version: Option[String] = None, hours: Int = 24): CdnResolver = {
        new SignedUrlCdnResolver(domain, version, urlSigner, hours, "https:")
      }

      def beCloseInTimeTo(date: Date, timeDiff: Int = 500) = new Matcher[Date] {
        def apply[D <: Date](e: Expectable[D]) =
          result((e.value.getTime - date.getTime) < timeDiff,
            "Dates are nearly at the same time",
            "Dates are different",
            e)
      }

      val sut = new CdnItemAssetResolver(mkSigningResolver(Some("//blah")))
    }

    "resolve" should {

      "return the file" in new withSigning {
        sut.resolve("123456789012345678901234:0")("FigurePattern.png") === "https://blah/player/item/123456789012345678901234:0/FigurePattern.png"
      }
    }

  }
}
