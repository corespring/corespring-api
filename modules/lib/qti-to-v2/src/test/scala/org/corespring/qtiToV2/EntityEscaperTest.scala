package org.corespring.qtiToV2

import org.specs2.mutable.Specification

class EntityEscaperTest extends Specification with EntityEscaper {

  import EntityEscaper._

  "escape" should {

    "convert decimal &#<int>; values to entity nodes" in {
      entities.map(entity => {
        escape(s"""&${entity.name};""") must be equalTo(s"""<entity value="${entity.unicode}"/>""")
      })
    }

    "convert &<string>; values to entity nodes" in {
      entities.map(entity => {
        escape(s"""&#${entity.unicode};""") must be equalTo(s"""<entity value="${entity.unicode}"/>""")
      })
    }

  }

}
