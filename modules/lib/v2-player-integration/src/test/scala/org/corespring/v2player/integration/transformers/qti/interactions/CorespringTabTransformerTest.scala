package org.corespring.v2player.integration.transformers.qti.interactions

import org.corespring.qtiToV2.interactions.CorespringTabTransformer
import org.specs2.mutable.Specification
import scala.xml.Node
import scala.xml.transform.RuleTransformer

class CorespringTabTransformerTest extends Specification {

  def qti(tabData: Map[String, Seq[Node]]) =
    <assessmentItem>
      <itemBody>
        <cs-tabs>
          { tabData.map { case (title, child) => <cs-tab title={ title }>{ child }</cs-tab> } }
        </cs-tabs>
      </itemBody>
    </assessmentItem>

  val tabData = Map(
    "tabOne" -> <div>This is the content of tab one</div>,
    "tabTwo" -> <div>...and this is the content of tab two</div>)

  "CorespringTabTransformer" should {

    val input = qti(tabData)
    val output = new RuleTransformer(CorespringTabTransformer).transform(input)

    "replace <cs-tabs/> with <corespring-tabs/>" in {
      (output \ "cs-tabs").length must be equalTo 0
      (input \ "cs-tabs").length must be equalTo (output \ "corespring-tabs").length
    }

    "replace <cs-tab/> with with <corespring-tab/>" in {
      (output \ "cs-tab").length must be equalTo 0
      (input \ "cs-tab").length must be equalTo (output \ "corespring-tab").length
    }

    "preserve children of <cs-tab/> in <corespring-tab/>" in {
      ((input \ "cs-tab") zip (output \ "corespring-tab")).map {
        case (oldNode, newNode) => {
          oldNode.child must be equalTo newNode.child
        }
      }
    }

  }

}
