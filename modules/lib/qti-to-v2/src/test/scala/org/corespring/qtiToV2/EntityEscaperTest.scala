package org.corespring.qtiToV2

import org.specs2.mutable.Specification

import scala.xml.XML

class EntityEscaperTest extends Specification with EntityEscaper {

  import EntityEscaper._

  "escapeEntities" should {

    "convert decimal &#<int>; values to entity nodes" in {
      entities.map(entity => {
        escapeEntities(s"""&${entity.name};""") must be equalTo(s"""<entity value="${entity.unicode}"/>""")
      })
    }

    "convert &<string>; values to entity nodes" in {
      entities.map(entity => {
        escapeEntities(s"""&#${entity.unicode};""") must be equalTo(s"""<entity value="${entity.unicode}"/>""")
      })
    }

  }

  "unescapeEntities" should {

    "convert entity nodes to decimal values" in {
      entities.map(entity => {
        unescapeEntities(s"""<entity value="${entity.unicode}"/>""") must be equalTo(s"&#${entity.unicode};")
      })
    }

  }

  "escape + xml + toString + unescape" should {

    "preserve original markup for unicode values" in {
      entities.map(entity => {
        unescapeEntities(XML.loadString(escapeEntities(s"&#${entity.unicode};")).toString) must be equalTo(s"&#${entity.unicode};")
      })
    }

    "convert entity strings to entity unicode" in {
      entities.map(entity => {
        unescapeEntities(XML.loadString(escapeEntities(s"&${entity.name};")).toString) must be equalTo(s"&#${entity.unicode};")
      })
    }

  }

}
