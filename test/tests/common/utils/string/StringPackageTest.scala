package tests.common.utils.string

import org.specs2.mutable.Specification
import common.utils.string

class StringPackageTest extends Specification {

  "string" should {

    "lower case first char" in {

      string.lowercaseFirstChar("Hello") === "hello"
      string.lowercaseFirstChar("") === ""
      string.lowercaseFirstChar(null) === null

    }
  }

}
