package org.corespring.api.v1.fieldValues

import org.specs2.mutable.Specification
import play.api.libs.json._

class QueryOptionsTest extends Specification {

  "QueryOptions" should {

    def assertExtraction(json: String, query: Option[String], filter: Option[String], skip: Int, limit: Int): Boolean = {
      val jsValue = Json.parse(json)

      val output: Option[Options] = QueryOptions.unapply(jsValue)

      output match {
        case Some(options) => {
          options.query == query &&
            options.filter == filter &&
            options.skip == skip &&
            options.limit == limit
        }
        case _ => false
      }
    }

    "extract valid json" in {
      val json = """{
          "q" : {"name" : "ed"},
          "f" : {"age" : "10"},
          "sk" : 100,
          "l" : 4
          }"""

      val assertion = assertExtraction(json,
        Some( """{"name":"ed"}"""),
        Some( """{"age":"10"}"""),
        100,
        4)

      if (assertion) success else failure
    }

    "extract valid json with defaults" in {
      val assertion = assertExtraction("{}", None, None, 0, 50)
      if (assertion) success else failure
    }

  }
}