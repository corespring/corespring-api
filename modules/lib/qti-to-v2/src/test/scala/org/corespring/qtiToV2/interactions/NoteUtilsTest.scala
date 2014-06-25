package org.corespring.qtiToV2.interactions

import org.specs2.mutable.Specification

class NoteUtilsTest extends Specification with NodeUtils {

  "whichFirst" should {

    "return correct value for sibling nodes" in {
      val xml =
        <body>
          <ul></ul>
          <div></div>
        </body>

      whichFirst(xml, "ul", "div") must be equalTo Some("ul")
    }

    "return correct value when first occurrence is in a deeper subtree" in {
      val xml =
        <html>
          <body>
            <ul>
              <li>I'm first</li>
            </ul>
            <span>I'm second</span>
          </body>
        </html>

      whichFirst(xml, "li", "span") must be equalTo Some("li")
    }

    "return correct value when first occurrance is in a more shallow subtree" in {
      val xml =
        <html>
          <body>
            <span>I'm first</span>
            <ul>
              <li>I'm second</li>
            </ul>
          </body>
        </html>

      whichFirst(xml, "li", "span") must be equalTo Some("span")
    }

    "return None when neither node is present in the tree" in {
      val xml =
        <html>
          <body>
            <span></span>
            <ul>
              <li></li>
            </ul>
          </body>
        </html>

      whichFirst(xml, "div", "table") must beNone
    }

  }

}
