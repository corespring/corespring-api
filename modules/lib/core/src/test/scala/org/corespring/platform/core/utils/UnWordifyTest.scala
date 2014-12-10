package org.corespring.platform.core.utils

import org.specs2.mutable.Specification


class UnWordifyTest extends Specification {

  import UnWordify._

  val quote = "There is nothing to fear but fear itself."

  "convertWordChars" should {

    "replace double curly quotes" in {
      s"“$quote”".convertWordChars must be equalTo(s"&ldquo;$quote&rdquo;")
    }

    "replace single curly quotes" in {
      s"‘$quote’".convertWordChars must be equalTo(s"&lsquo;$quote&rsquo;")
    }

    "replace ndash" in {
      "–2 + 2 = 0".convertWordChars must be equalTo("&ndash;2 + 2 = 0")
    }

    "replace mdash" in {
      "—2 + 2 = 0".convertWordChars must be equalTo("&mdash;2 + 2 = 0")
    }

  }

}
