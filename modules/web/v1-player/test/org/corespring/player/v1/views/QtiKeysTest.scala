package org.corespring.player.v1.views

import org.corespring.player.v1.views.models.QtiKeys
import org.specs2.mutable.Specification

class QtiKeysTest extends Specification {

  "qti keys" should {

    "create empty keys" in {
      val xml = <itemBody></itemBody>
      QtiKeys(xml).keys === Seq()
    }

    "create keys" in {
      val xml = <itemBody>
        <choiceInteraction></choiceInteraction>
        <textEntryInteraction></textEntryInteraction>
      </itemBody>
      QtiKeys(xml).keys.sorted === Seq("choiceInteraction", "textEntryInteraction")
    }

    "find keys in feedback nodes" in {
      val xml = <itemBody>
        <choiceInteraction>
          <simpleChoice>
            <feedbackBlock><math></math></feedbackBlock>
          </simpleChoice>
        </choiceInteraction>
      </itemBody>
      QtiKeys(xml).keys.sorted === Seq("choiceInteraction", "math")
    }

    "find tabs" in {
      QtiKeys(<n><div cs-tabs="true"></div></n>).keys === Seq("tabs")
      QtiKeys(<n><div tabs="true"></div></n>).keys === Seq("tabs")
    }

    "find numbered lines" in {
      QtiKeys(<n><div class="numbered-lines"/></n>).keys === Seq("numberedLines")
    }
  }
}
