package org.corespring.assets

import org.specs2.mutable.Specification
import org.apache.commons.httpclient.util.URIUtil

class EncodingHelperTest extends Specification {

  //https://www.ietf.org/rfc/rfc3986.txt
  val rfc3986Reserved = ":/?#[]@!$&'()*+,;="

  val helper = new EncodingHelper()

  def encode(s: String, count: Int) = count match {
    case 0 => s
    case c if c > 0 => (1 to count).foldRight(s)((index: Int, acc: String) => URIUtil.encodePath(acc))
  }

  "encodedOnce" should {

    def assertEncodedOnce(s: String, count: Int) = {
      val expected = URIUtil.encodePath(helper.decodeCompletely(s), "utf-8")
      val encoded = encode(s, count)

      s"encodedOnce('$encoded') => '$expected'" in {
        helper.encodedOnce(encoded) must_== expected
      }
    }

    assertEncodedOnce(rfc3986Reserved, 0)
    assertEncodedOnce(rfc3986Reserved, 1)
    assertEncodedOnce(rfc3986Reserved, 2)
    assertEncodedOnce(rfc3986Reserved, 3)

    assertEncodedOnce("hi there", 0)
    assertEncodedOnce("hi there", 1)
    assertEncodedOnce("hi there", 2)
    assertEncodedOnce("hi there", 5)

    assertEncodedOnce("/a/b/c.png", 0)
    assertEncodedOnce("/a/b/c.png", 1)
    assertEncodedOnce("/a/b/c.png", 2)
    assertEncodedOnce("/a~tilde/b/c.png", 2)

  }

  "decodeCompletely" should {

    def assertDecodeCompletely(s: String, encodeCount: Int = 1) = {

      val encoded = encode(s, encodeCount)
      s"decodeCompletely('$encoded') => $s" in {
        helper.decodeCompletely(encoded) must_== s
      }
    }
    //Note: + is treated as a space on decode.
    assertDecodeCompletely(rfc3986Reserved.replace("+", " "))
    assertDecodeCompletely(rfc3986Reserved.replace("+", " "), 10)
    assertDecodeCompletely("hi how are you $/there !/test")
  }
}
