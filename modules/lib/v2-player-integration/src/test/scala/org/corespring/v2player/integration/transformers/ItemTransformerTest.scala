package org.corespring.v2player.integration.transformers

import org.corespring.platform.core.models.item.resource.{VirtualFile, Resource}
import org.corespring.platform.core.models.item.{TaskInfo, Item}
import org.specs2.mutable.Specification
import play.api.libs.json.JsObject

class ItemTransformerTest extends Specification{

  val qti =
    <assessmentItem>
      <responseDeclaration identifier="Q_01" cardinality="single" baseType="identifier">
        <correctResponse>
          <value>ChoiceA</value>
        </correctResponse>
      </responseDeclaration>
      <itemBody>
        <choiceInteraction responseIdentifier="Q_01" shuffle="false" maxChoices="1">
          <prompt>ITEM PROMPT?</prompt>
          <simpleChoice identifier="ChoiceA">ChoiceA text (Correct Choice)
            <feedbackInline identifier="ChoiceA" defaultFeedback="true"/>
          </simpleChoice>
          <simpleChoice identifier="ChoiceD">ChoiceD text
            <feedbackInline identifier="ChoiceD" defaultFeedback="true"/>
          </simpleChoice>
        </choiceInteraction>
      </itemBody>
    </assessmentItem>

  "Item transformer" should {
    "transform an item to poc json" in {

      val item = Item(
        taskInfo = Some(TaskInfo( title = Some("item one"))),
        data = Some(
          Resource(
            name = "data",
            files = Seq(
              VirtualFile(
                 name = "qti.xml",
                 contentType = "text/xml",
                 content = qti.toString
              )
            )
          )
        )
      )

      val json = ItemTransformer.transformToV2Json(item)

      (json \ "metadata" \ "title").as[String] === "item one"

      (json \ "components" \ "Q_01").asOpt[JsObject] must beSome[JsObject]
    }
  }

}
