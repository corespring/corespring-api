package org.corespring.container.client.controllers

import org.specs2.mutable.Specification
import org.specs2.specification.Scope

class TimestampPathTest extends Specification {

  trait scope extends Scope with TimestampPath {

    override def getTimestamp = "stamp"
  }

  "timestamped" should {

    "stamp path" in new scope {
      timestamped("a/b/c/d/e.png") must_== "a/b/c/d/stamp-e.png"
    }

    "stamp name only" in new scope {
      timestamped("e.png") must_== "stamp-e.png"
    }

    "stamp name with no suffix" in new scope {
      timestamped("e") must_== "stamp-e"
    }
  }
}
