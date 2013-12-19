package org.corespring.qti.models.interactions

import org.specs2.mutable.Specification
import org.corespring.qti.models.responses.StringResponse
import play.api.libs.json.{JsString, JsObject}

class InlineChoiceInteractionTest extends Specification {

  "toJs" should {

    val interaction = InlineChoiceInteraction(<inlineChoiceInteraction responseIdentifier="id"></inlineChoiceInteraction>)

    "convert to JsObject" in {
      val response = StringResponse("Q1", "test")
      val result = interaction.toJs(response)

      result match {
        case Some((script:String,jsObject: JsObject)) => {
          (jsObject \ "value").asInstanceOf[JsString].value === "test"
          success
        }
        case _ => failure("returned no result")
      }

    }

  }

}
