package org.corespring.platform.core.models

import org.specs2.mutable.Specification
import org.corespring.test.PlaySingleton

class StandardTest extends Specification {

  PlaySingleton.start()

  "group" should {

    def standard(dotNotation: String): Standard = Standard(dotNotation = Some(dotNotation))

    "return None when there is no dotNotation" in {
      Standard(dotNotation = None).group === None
    }

    "return group without trailing lowercase letter" in {
      standard("RF.1.3a").group.get === "RF.1.3"
    }

    "return group rounded to first 3 groupings between periods" in {
      standard("3.OA.A.4").group.get === "3.OA.A"
    }

    "return group rouded to first 3 groupings between periods even with trailing lowercase letter" in {
      standard("3.NF.A.3a").group.get === "3.NF.A"
    }

  }

}
