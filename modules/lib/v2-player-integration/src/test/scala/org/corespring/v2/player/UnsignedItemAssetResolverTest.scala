package org.corespring.v2.player

import java.util.Date

import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

class UnsignedItemAssetResolverTest extends Specification with Mockito {

  "UnsignedItemAssetResolver" should {

    trait scope extends Scope {
      def mkCdnResolver(domain: Option[String]) = {
        new CDNResolver(domain, None)
      }
    }

    "resolve" should {

      "return resolved url" in new scope {
        val sut = new UnsignedItemAssetResolver(mkCdnResolver(Some("//valid-domain")))
        sut.resolve("123456789012345678901234:0")("test.jpeg") === "//valid-domain/123456789012345678901234/0/data/test.jpeg"
      }

      "return file when cdnDomain is not defined" in new scope {
        val sut = new UnsignedItemAssetResolver(mkCdnResolver(None))
        sut.resolve("123456789012345678901234:0")("test.jpeg") === "test.jpeg"
      }

      "return file when cdnDomain does not start with two slashes" in new scope {
        val sut = new UnsignedItemAssetResolver(mkCdnResolver(Some("invalid-domain")))
        sut.resolve("123456789012345678901234:0")("test.jpeg") === "test.jpeg"
      }

    }

  }
}
