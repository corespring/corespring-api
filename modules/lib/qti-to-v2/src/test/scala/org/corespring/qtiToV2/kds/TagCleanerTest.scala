package org.corespring.qtiToV2.kds

import org.specs2.mutable.Specification

class TagCleanerTest extends Specification {

  "clean" should {

    "convert self-terminating tags" in {
      TagCleaner.clean(<corespring-teacher-instructions id="3"/>).toString must be equalTo("""<corespring-teacher-instructions id="3"></corespring-teacher-instructions>""")
    }

    "transform child tags recursively" in {
      TagCleaner.clean(<div><corespring-teacher-instructions id="1"/><corespring-teacher-instructions id="2"/></div>).toString must be equalTo("""<div><corespring-teacher-instructions id="1"></corespring-teacher-instructions><corespring-teacher-instructions id="2"></corespring-teacher-instructions></div>""")
    }

  }

}
