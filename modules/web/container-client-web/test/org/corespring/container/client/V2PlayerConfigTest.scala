package org.corespring.container.client

import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification._

class V2PlayerConfigTest extends Specification with Mockito {

  class scope extends Scope

  "apply" should {

    "set useNewRelic to false, new relic config is None" in new scope {
      V2PlayerConfig(None, None).useNewRelic === false
    }

  }
}