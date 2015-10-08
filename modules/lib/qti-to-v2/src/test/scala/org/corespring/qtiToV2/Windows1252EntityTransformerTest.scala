package org.corespring.qtiToV2

import org.specs2.mutable.Specification

class Windows1252EntityTransformerTest extends Specification {

  "transform" should {

    "convert '…' to '&hellip;'" in {
      Windows1252EntityTransformer.transform("…") must be equalTo("&hellip;")
    }

    "convert '€' to '&euro;'" in {
      Windows1252EntityTransformer.transform("€") must be equalTo("&euro;")
    }

    "convert 'ƒ' to '&fnof;'" in {
      Windows1252EntityTransformer.transform("ƒ") must be equalTo("&fnof;")
    }

    "convert '‘' to '&lsquo;'" in {
      Windows1252EntityTransformer.transform("‘") must be equalTo("&lsquo;")
    }

    "convert '’' to '&rsquo;'" in {
      Windows1252EntityTransformer.transform("’") must be equalTo("&rsquo;")
    }

    "convert '“' to '&ldquo;'" in {
      Windows1252EntityTransformer.transform("“") must be equalTo("&ldquo;")
    }

    "convert '”' to '&rdquo;'" in {
      Windows1252EntityTransformer.transform("”") must be equalTo("&rdquo;")
    }

  }

}
