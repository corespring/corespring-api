package org.corespring.drafts.item

import org.specs2.mock.Mockito
import org.specs2.mutable.Specification

class S3PathsTest extends Specification with Mockito {

  val sut = S3Paths

  "S3Paths" should {

    "itemFromStringId" should {

      "replace colon with slash in full id" in {
          val result = sut.itemFromStringId("1234:567")
          result === "1234/567"
      }

      "append default version 0 if no version in id" in {
        val result = sut.itemFromStringId("1234")
        result === "1234/0"
      }

    }
  }
}
