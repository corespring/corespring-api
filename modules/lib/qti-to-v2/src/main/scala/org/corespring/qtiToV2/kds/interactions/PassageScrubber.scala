package org.corespring.qtiToV2.kds.interactions

trait PassageScrubber {

  /**
   * KDS passages contain invalid </video> tag XML. They are probably unaware of this because it's in CDATA, so any
   * plain old XML parser won't complain about it. When it hits the user's browser, their browser can probably render
   * it, but Scala's XML parser is a lot more strict, so we have to deal with it. Good stuff.
   */
  def scrub(xml: String) = xml.cleanVideoTags.cleanSourceTags

  private implicit class Scrubber(xml: String) {

    // Video tags contain controls attribute with no value, e.g., <video controls>
    def cleanVideoTags = """(?s)<video(.*?)(controls)(.*?)>""".r.replaceAllIn(xml, "<video$1$3>")

    // Source tags are not self terminated, e.g., <source><otherMarkup/>
    def cleanSourceTags = """(?s)<source(.*?)>""".r.replaceAllIn(xml, "<source$1/>")

  }

}
