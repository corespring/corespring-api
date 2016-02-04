package org.corespring.itemSearch

import java.net.URL

import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import scala.concurrent.ExecutionContext
import scalaz.Success

class ElasticSearchItemIndexServiceTest extends Specification with Mockito {

  trait scope extends Scope {

    lazy val config = new ElasticSearchConfig(new URL("http://localhost"), "", "")
    lazy val service = new ElasticSearchItemIndexService(
      config,
      rawTypes = Seq.empty,
      executionContext = new ElasticSearchExecutionContext(ExecutionContext.Implicits.global))
  }

  "search" should {

    "return an empty result set for a query with no collections" in new scope {
      service.search(ItemIndexQuery()) must equalTo(Success(ItemIndexSearchResult(0, Seq.empty))).await
    }
  }
}
