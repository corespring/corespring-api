package org.corespring.models

import org.specs2.mutable.Specification

class DisplayConfigTest extends Specification {

  "DisplayConfig" should {

    "iconSet" should {

      "with invalid value" should {

        "sets value to default" in {
          DisplayConfig(iconSet = "invalid", colors = ColorPalette.default)
            .iconSet must be equalTo(DisplayConfig.Defaults.iconSet)
        }

      }

      "with valid value" should {

        "set value" in {
          DisplayConfig.IconSets.sets.map{ iconSet => {
            DisplayConfig(iconSet = iconSet, colors = ColorPalette.default)
              .iconSet must be equalTo(iconSet)
          }}.tail
        }

      }


    }

  }

}
