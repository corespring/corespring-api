package org.corespring.container.client.controllers.helpers

import org.specs2.mutable.Specification
import org.specs2.specification.Scope

class PlayerXhtmlTest extends Specification {

  implicit class WithoutCarriageReturn(s: String) {
    def withoutCarriageReturn = s.replaceAll("\r", "")
  }

  val comps = Seq("corespring-apple", "corespring-banana", "corespring-one", "corespring-two")

  case class assertPlayerXhtml(in: String, expected: String) extends Scope {
    PlayerXhtml.mkXhtml(comps, in).trim.withoutCarriageReturn === expected.withoutCarriageReturn
  }

  "preparePlayerXhtml" should {

    "preserves whitespace" in assertPlayerXhtml("<div> <p></p> </div>", """<div> <div class="para"></div> </div>""")
    "not strip space after <em>" in assertPlayerXhtml("<em></em> a", "<em></em> a")
    "not strip space after <i>" in assertPlayerXhtml("<i>a</i> a", "<i>a</i> a")

    "when changing the custom tags to divs with an attribute" should {
      "change the <tag> to <div tag=''>" in assertPlayerXhtml(
        """<corespring-apple id="1"></corespring-apple><corespring-banana id="2"></corespring-banana>""",
        """<div id="1" corespring-apple=""></div><div id="2" corespring-banana=""></div>""")

      "change nested tags" in assertPlayerXhtml(
        "<corespring-one><corespring-two>Content</corespring-two></corespring-one>",
        """<div corespring-one=""><div corespring-two="">Content</div></div>""")

      "not split tags" in assertPlayerXhtml(
        """<p>Hello
          |<corespring-one><corespring-two>Content</corespring-two></corespring-one>World</p>""".stripMargin,
        """<div class="para">Hello
          |<div corespring-one=""><div corespring-two="">Content</div></div>World</div>""".stripMargin)
    }

    "convert <p> to <div class=\"para\">" in assertPlayerXhtml(
      "<p>Hello</p>",
      """<div class="para">Hello</div>""")

    "convert <p class='a'> to <div class='para a'>" in assertPlayerXhtml(
      """<p class="p-intro2">Hello</p>""",
      """<div class="para p-intro2">Hello</div>""")

  }

}
