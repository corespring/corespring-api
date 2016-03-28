package org.corespring.models

import org.specs2.mutable.Specification

class DisplayConfigTest extends Specification {

  "DisplayConfig" should {

    "with invalid iconSet value" should {

      "sets iconSet value to default" in {
        DisplayConfig(iconSet = "invalid", colors = ColorPalette.default)
          .iconSet must be equalTo(DisplayConfig.Defaults.iconSet)
      }

    }

  }

}
