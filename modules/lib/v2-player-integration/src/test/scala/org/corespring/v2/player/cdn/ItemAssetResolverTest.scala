package org.corespring.v2.player.cdn

import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

class ItemAssetResolverTest extends Specification with Mockito {

  "ItemAssetResolver" should {

    trait scope extends Scope {

      val sut = new ItemAssetResolver {
      }
    }

    "resolve" should {

      "return the file" in new scope {
        sut.resolve("123456789012345678901234:0")("FigurePattern.png") === "/player/item/123456789012345678901234:0/FigurePattern.png"
      }
    }
  }
}
