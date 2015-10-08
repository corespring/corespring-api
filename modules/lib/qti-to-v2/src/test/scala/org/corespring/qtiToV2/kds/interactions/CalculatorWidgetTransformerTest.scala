package org.corespring.qtiToV2.kds.interactions

import org.specs2.mutable.Specification

class CalculatorWidgetTransformerTest extends Specification {

  import CalculatorWidgetTransformer._

  def manifestFor(calculatorType: String) =
    <resource>
      <metadata>
        <lom>
          <general>
            <mathTools>
              <calculator>{ calculatorType }</calculator>
            </mathTools>
          </general>
        </lom>
      </metadata>
    </resource>

  "interactionJs" should {

    "return empty interaction for 'advanced'" in {
      interactionJs(<qti></qti>, manifestFor("advanced")) must be equalTo (Map.empty)
    }

    "return component for 'basic'" in {
      interactionJs(<qti></qti>, manifestFor("basic")) must be equalTo (calculatorOfType("basic"))
    }

    "return component for 'scientific'" in {
      interactionJs(<qti></qti>, manifestFor("scientific")) must be equalTo (calculatorOfType("scientific"))
    }

  }

  "transform" should {

    val qti = <itemBody></itemBody>

    "add no child to <itemBody/> for 'advanced'" in {
      transform(qti, manifestFor("advanced")) must be equalTo (qti)
    }

    "add <corespring-calculator/> child to <itemBody/> for 'basic'" in {
      transform(qti, manifestFor("basic")) must be equalTo (<itemBody>{ calculatorNode }</itemBody>)
    }

    "add <corespring-calculator/> child to <itemBody/> for 'scientific'" in {
      transform(qti, manifestFor("scientific")) must be equalTo (<itemBody>{ calculatorNode }</itemBody>)
    }

  }

}
