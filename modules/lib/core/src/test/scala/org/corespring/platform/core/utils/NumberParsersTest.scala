package org.corespring.platform.core.utils

import org.specs2.mutable.{Specification}

class NumberParsersTest extends Specification
{
  val numberParser = new NumberParsers {}

  "number parser" should {
    "parse int safely" in {
      numberParser.parseInt("3") === Some(3)
      numberParser.parseInt("apple") === None
      numberParser.intValueOrZero("3") === 3
      numberParser.intValueOrZero("apple") === 0
    }

    "parse float safely" in {
      numberParser.parseFloat("3") === Some(3.0)
      numberParser.parseFloat("apple") === None
      numberParser.floatValueOrZero("3") === 3
      numberParser.floatValueOrZero("apple") === 0
    }
  }
}
