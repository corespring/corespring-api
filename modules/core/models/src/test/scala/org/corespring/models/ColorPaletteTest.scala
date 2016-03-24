package org.corespring.models

import org.specs2.mutable.Specification

class ColorPaletteTest extends Specification {

  "lighten" should {

    "convert to lighter color" in {
      ColorPalette.lighten("#84A783", 0.45) must be equalTo("#C7D7C7")
    }

  }

}