package org.corespring.v2.player

import java.util.Date

import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

class UnsignedItemAssetResolverTest extends Specification with Mockito {

  "UnsignedItemAssetResolver" should {

    trait scope extends Scope {
      val cdnResolver = {
        val m = mock[CDNResolver]
        m.resolveDomain(any[String]) returns "fake resolved domain"
        m
      }

      val sut = new UnsignedItemAssetResolver(cdnResolver)
    }

    "on creation" should {

      "not throw when all parameters are set" in new scope {
        sut should haveClass[UnsignedItemAssetResolver]
      }

      "on calling resolve" should {

        "return resolved url" in new scope {
          sut.resolve("123456789012345678901234:0")("test.jpeg") === "fake resolved domain"
        }

        "use correct s3 path" in new scope {
          val itemId = "123456789012345678901234"
          val itemVersion = 0
          val file = "test.jpeg"
          sut.resolve(itemId + ":" + itemVersion)(file)
          there was one(cdnResolver).resolveDomain(find(itemId + "/" + itemVersion + "/data/" + file))
        }

      }
    }

  }
}
