package org.corespring.platform.core.services

import org.specs2.mutable.Specification

class StandardQueryBuilderTest extends Specification {

  class scope extends StandardQueryBuilder

  "getStandardBySearchQuery" should {

    "return filters in the dbo" in new scope {
      val dbo = getStandardBySearchQuery("""{"filters" : {"subject" : "ELA"}}""").get
      dbo.get("subject") === "ELA"
    }
  }
}
