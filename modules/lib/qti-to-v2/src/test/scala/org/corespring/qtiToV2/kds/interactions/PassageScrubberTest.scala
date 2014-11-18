package org.corespring.qtiToV2.kds.interactions

import org.specs2.mutable.Specification

import scala.xml.XML


class PassageScrubberTest extends Specification with PassageScrubber {

  val badMarkup =
    """<video width="500" height="500" controls>
        <source src="./videos/662604p3.mp4" type="video/mp4">
        <span class="noprint">Your browser does not support the video tag.</span>
      </video>
    """

  "scrub" should {

    val result = scrub(badMarkup)

    "produce valid markup" in {
      try {
        XML.loadString(result)
      } catch {
        case e: Exception => failure
      }
      success
    }

  }

}
