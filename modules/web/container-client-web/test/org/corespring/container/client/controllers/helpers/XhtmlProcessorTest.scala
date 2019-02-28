package org.corespring.container.client.controllers.helpers

import org.specs2.mutable.Specification
import org.specs2.specification.Scope

class XhtmlProcessorTest extends Specification {

  import XhtmlProcessor._

  case class assertWellFormed(s: String, expected: Option[String] = None) extends Scope {
    val e = expected.getOrElse(s)
    val wellFormed = toWellFormedXhtml(s)
    wellFormed === e
    isValidXml(wellFormed) === true
  }

  "XhtmlProcessor" should {

    "toWellFormedXhtml" should {

      "not change valid xhtml" in assertWellFormed(
        """
          |<div>
          |  hello there
          |</div>
        """.stripMargin)

      "preserves white space" in assertWellFormed(" <br> ", Some(" <br /> "))
      "preserves double white spaces" in assertWellFormed("  <br>  ", Some("  <br />  "))
      "fixes brs" in assertWellFormed("<div><br> a</div>", Some("<div><br /> a</div>"))
      "doesn't strip white space in <em>" in assertWellFormed("<div><br /><em>a</em> a</div>")
      "doesn't strip white space in <i>" in assertWellFormed("<div>what does <i>extracting</i> mean</div>")
      "wrap markup if needed" in assertWellFormed("apple <br/>", Some("<div>apple <br /></div>"))
      "preserves style tags" in assertWellFormed("""<div class="itemBody qti"><style type="text/css">body { color: #fff; }</style></div>""")

      "throw an error if you attempt to use a tag other than div or span" in {
        toWellFormedXhtml("a", "blah") must throwA[IllegalArgumentException]
      }

    }

  }

}
