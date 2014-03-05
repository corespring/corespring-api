package org.corespring.search.indexing

import org.specs2.mutable.Specification
import com.fasterxml.jackson.databind.ObjectMapper

class ContentMappingTest extends Specification {


  val mapper = new ObjectMapper()

  "generated mapping" should {
    "be the same as the json mapping" in {
      val res = getClass.getResource("mapping.json")
      val json = mapper.readTree(res)
      val generatedMapping = ContentMapping.generate
      json === mapper.readTree(generatedMapping._source.string)
    }
  }

}
