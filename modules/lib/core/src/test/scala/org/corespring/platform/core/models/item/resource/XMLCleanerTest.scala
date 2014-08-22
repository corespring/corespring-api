package org.corespring.platform.core.models.item.resource

import org.specs2.mutable.Specification

class XMLCleanerTest extends Specification {

  "clean" should {

    "remove leading/tailing whitespace" in {
      val xml = " <itemBody></itemBody> "
      XMLCleaner.clean(xml) === xml.trim
    }

  }

}
