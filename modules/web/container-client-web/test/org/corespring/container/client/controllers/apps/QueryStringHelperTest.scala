package org.corespring.container.client.controllers.apps

import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.test.FakeRequest

class QueryStringHelperTest extends Specification {

  trait scope extends Scope with QueryStringHelper {
    def params(queryString: String) = {
      mkQueryParams(params => params.mkString(","))(FakeRequest("", queryString))
    }
  }

  "mkQueryParams" should {

    "create the params" in new scope {
      params("?a=b&c=d") must_== "a -> b,c -> d"
    }

    "strips default params" in new scope {
      params("?loggingEnabled=true&logCategory=cat&mode=b&c=d") must_== "c -> d"
    }
  }
}
