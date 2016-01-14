package org.corespring.v2.player.cdn

import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

class DisabledItemAssetResolverTest extends Specification with Mockito {

  "DisabledItemAssetResolver" should {

    trait scope extends Scope {

      val sut = new DisabledItemAssetResolver
    }

    "resolve" should {

      "return the file" in new scope {
        sut.resolve("123456789012345678901234:0")("test.jpeg") === "test.jpeg"
      }
    }
  }
}
