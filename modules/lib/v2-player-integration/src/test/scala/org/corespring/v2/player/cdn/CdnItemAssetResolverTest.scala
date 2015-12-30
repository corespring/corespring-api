package org.corespring.v2.player.cdn

import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

class CdnItemAssetResolverTest extends Specification with Mockito {

  "CdnItemAssetResolver" should {

    trait scope extends Scope {

      val cdnResolver = {
        val m = mock[CdnResolver]
        m.resolveDomain(any[String]) returns "fake resolved url"
        m
      }


      val sut = new CdnItemAssetResolver(cdnResolver)
    }

    "resolve" should {

      "pass correct s3 path to cdnResolver" in new scope {
        sut.resolve("123456789012345678901234:0")("test.jpeg")
        there was one(cdnResolver).resolveDomain(find("123456789012345678901234/0/data/test.jpeg"))
      }
    }
  }
}
