package org.corespring.v2.player.assets

import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import play.api.mvc.Results

class ResultHeadersTest extends Specification with ResultHeaders with Mockito with Results {

  "withContentHeaders" should {

    "svgx extension" should {

      val result = Ok.withContentHeaders("my-svg.svgx")

      "include Content-Type 'image/svg+xml'" in {
        result.header.headers.get("Content-Type") must be equalTo (Some("image/svg+xml"))
      }

      "include Content-Encoding 'gzip'" in {
        result.header.headers.get("Content-Encoding") must be equalTo (Some("gzip"))
      }

      "include Vary 'Accept-Encoding'" in {
        result.header.headers.get("Vary") must be equalTo (Some("Accept-Encoding"))
      }

    }

  }

}
