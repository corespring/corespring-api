package org.corespring.qtiToV2.kds.interactions

import org.specs2.mutable.Specification

import scala.xml.Node

class ProtractorWidgetTransformerTest extends Specification {

  import ProtractorWidgetTransformer._

  def manifestFor(tool: Option[Node]) =
    <resource>
      <metadata>
        <lom>
          <general>
            <mathTools>
              { tool.getOrElse("") }
            </mathTools>
          </general>
        </lom>
      </metadata>
    </resource>

  "interactionJs" should {

    "return nothing when there is no protractor" in {
      interactionJs(<qti></qti>, manifestFor(None)) must be equalTo (Map.empty)
    }

    "return protractor JSON when there is a protractor" in {
      interactionJs(<qti></qti>, manifestFor(Some(<mathTool>protractor</mathTool>))) must be equalTo (protractorJson)
    }

  }

  "transform" should {

    val qti = <itemBody></itemBody>

    "add no child to <itemBody/> when there is no protractor" in {
      transform(qti, manifestFor(None)) must be equalTo (qti)
    }

    "add <corespring-protractor/> child to <itemBody/>" in {
      transform(
        qti, manifestFor(Some(<mathTool>protractor</mathTool>))) must be equalTo (<itemBody>{ protractorNode }</itemBody>)
    }

  }

}
