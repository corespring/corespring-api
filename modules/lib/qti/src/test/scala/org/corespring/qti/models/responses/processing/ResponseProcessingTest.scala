package org.corespring.qti.models.responses.processing

import org.specs2.mutable.Specification
import play.api.libs.json._
import org.corespring.qti.models.ItemBody
import org.corespring.qti.models.responses.{StringResponse, ArrayResponse}

class ResponseProcessingTest extends Specification {

  val itemBody = ItemBody(<itemBody/>)

  "ResponseProcessing" should {

    "ignore script without type='script' attribute" in {
      val node =
        <responseProcessing>
          <script type="text/javascript">
            console.log("Ignore me!");
          </script>
        </responseProcessing>
      ResponseProcessing(itemBody, node).script match {
        case None => success
        case _ => failure
      }
    }

    "have script with type='script' attribute" in {
      val node =
        <responseProcessing type="script">
          <script type="text/javascript">
            console.log("Hello, world!");
          </script>
        </responseProcessing>

      ResponseProcessing(itemBody, node).script match {
        case Some(script) => success
        case _ => failure
      }
    }

    "process" should {

      "do nothing without script" in {
        val node =
          <responseProcessing>
            <script type="text/javascript">
              myVar;
            </script>
          </responseProcessing>

        ResponseProcessing(itemBody, node).process(Some(Map("myVar" -> 1))) === None
      }

      "return something with script" in {
        val node =
          <responseProcessing type="script">
            <script type="text/javascript">
              myVar;
            </script>
          </responseProcessing>

        ResponseProcessing(itemBody, node).process(Some(Map("myVar" -> 1))) match {
          case None => failure
          case _ => success
        }
      }

      "return JsObject with correct value from script" in {
        val node =
          <responseProcessing type="script">
            <script type="text/javascript">
              <![CDATA[
                var response = { score: 5 };
                response;
              ]]>
            </script>
          </responseProcessing>
        ResponseProcessing(itemBody, node).process(Some(Map.empty[String, Any])) match {
          case Some((script:String,jsObject: JsObject)) if (jsObject \ "score").asInstanceOf[JsNumber].value == 5 => success
          case _ => failure
        }
      }

      "provide response variables for DragAndDropInteraction" in {
        val itemBody = ItemBody(
          <itemBody>
            <dragAndDropInteraction responseIdentifier='Q1'>
              <draggableChoice identifier='1'/>
              <draggableChoice identifier='2'/>
            </dragAndDropInteraction>
          </itemBody>
        )

        val responseProcessing = ResponseProcessing(itemBody,
          <responseProcessing type="script">
            <script type="text/javascript">
              Q1.value.target1;
            </script>
          </responseProcessing>)

        val response = ArrayResponse("Q1", Seq("target1:test"))
        responseProcessing.process(responses = Some(Seq(response))) match {
          case Some((script:String,string: JsString)) if string.value == "test" => success
          case _ => failure("did not produce response")
        }
      }

      "provide response variables for InlineChoiceInteraction" in {
        val itemBody = ItemBody(
          <itemBody>
            <inlineChoiceInteraction responseIdentifier='Q1'/>
          </itemBody>
        )

        val responseProcessing = ResponseProcessing(itemBody,
          <responseProcessing type="script">
            <script type="text/javascript">
              Q1.value;
            </script>
          </responseProcessing>
        )

        val response = StringResponse("Q1", "test")
        responseProcessing.process(responses = Some(Seq(response))) match {
          case Some((script:String,string: JsString)) if string.value == "test" => success
          case _ => failure("did not produce response")
        }

      }

      "provide response variables for LineInteraction" in {
        val itemBody = ItemBody(
          <itemBody>
            <lineInteraction responseIdentifier='Q1'/>
          </itemBody>
        )

        val responseProcessing = ResponseProcessing(itemBody,
          <responseProcessing type="script">
            <script type="text/javascript">
              Q1;
            </script>
          </responseProcessing>
        )

        val answer = Seq("0,0", "1,1")
        val response = ArrayResponse("Q1", answer)

        responseProcessing.process(responses = Some(Seq(response))) match {
          case Some((script:String,jsObject: JsObject)) if (jsObject \ "value").as[JsArray].value.map(_.as[String]) == answer => success
          case _ => failure
        }
      }

      "provides response variables for FocusTaskInteraction" in {
        testResponse(
          ItemBody(
            <itemBody>
              <focusTaskInteraction responseIdentifier='RESPONSE' checkIfCorrect='yes' itemShape='square' minSelections='2' maxSelections='2' shuffle='false'>
                <focusChoice identifier='1'/>
                <focusChoice identifier='2'/>
                <focusChoice identifier='3'/>
              </focusTaskInteraction>
            </itemBody>
          )
        )
      }

      "provides response variables for PointInteraction" in {
        testResponse(
          ItemBody(
            <itemBody>
              <pointInteraction responseIdentifier='RESPONSE'>
              </pointInteraction>
            </itemBody>
          )
        )
      }

      "provides response variables for OrderInteraction" in {
        testResponse(
          ItemBody(
            <itemBody>
              <orderInteraction responseIdentifier='RESPONSE' shuffle='true'>
                <simpleChoice identifier='1'/>
                <simpleChoice identifier='2'/>
                <simpleChoice identifier='3'/>
              </orderInteraction>
            </itemBody>
          )
        )
      }

      "provides response variables for ChoiceInteraction" in {
        testResponse(
          ItemBody(
            <itemBody>
              <choiceInteraction responseIdentifier='RESPONSE' shuffle='true' maxChoices='0'>
                <simpleChoice identifier='1' fixed='false'/>
                <simpleChoice identifier='2' fixed='false'/>
                <simpleChoice identifier='3' fixed='false'/>
              </choiceInteraction>
            </itemBody>
          )
        )
      }

      "provides response variables for SelectTextInteraction" in {
        val itemBody = ItemBody(
          <itemBody>
            <selectTextInteraction responseIdentifier='RESPONSE' selectionType='word' checkIfCorrect='yes'
                                   minSelections='2' maxSelections='2'></selectTextInteraction>
          </itemBody>
        )

        val responseProcessing = ResponseProcessing(itemBody,
          <responseProcessing type="script">
            <script type="text/javascript">
              RESPONSE.value;
            </script>
          </responseProcessing>
        )

        val response = StringResponse("RESPONSE", "test")
        responseProcessing.process(responses = Some(Seq(response))) match {
          case Some((script:String,string: JsString)) if string.value == "test" => success
          case _ => failure("did not produce response")
        }

      }

      def testResponse(itemBody: ItemBody) = {

        val responseProcessing = ResponseProcessing(itemBody,
          <responseProcessing type="script">
            <script type="text/javascript">
              RESPONSE;
            </script>
          </responseProcessing>
        )

        val answer = Seq("1", "2")
        val response = ArrayResponse("RESPONSE", answer)

        responseProcessing.process(responses = Some(Seq(response))) match {
          case Some((script:String,jsObject: JsObject)) => {
            (jsObject \ "value").as[JsArray].value.map(_.as[String]) === answer
            success
          }
          case _ => failure
        }

      }

      "merge variables and outcomes" in {
        val itemBody = ItemBody(
          <itemBody>
            <inlineChoiceInteraction responseIdentifier='Q1'/>
          </itemBody>
        )

        val responseProcessing = ResponseProcessing(itemBody,
          <responseProcessing type="script">
            <script type="text/javascript">
              Q1;
            </script>
          </responseProcessing>)

        val response = StringResponse("Q1", "test")

        val variables = Some(
          Map(
            "Q1" -> Json.obj(
              "outcome" -> Json.obj(
                "score" -> JsNumber(1),
                "isCorrect" -> JsBoolean(true),
                "isComplete" -> JsBoolean(true)
              )
            )
          )
        )

        responseProcessing.process(variables = variables, responses = Some(Seq(response))) match {
          case Some((script:String,jsObject: JsObject)) => {
            (jsObject \ "outcome" \ "score").asInstanceOf[JsNumber].value === 1
            (jsObject \ "outcome" \ "isCorrect").asInstanceOf[JsBoolean].value === true
            (jsObject \ "outcome" \ "isComplete").asInstanceOf[JsBoolean].value === true
            (jsObject \ "value").asInstanceOf[JsString].value === "test"
            success
          }
          case _ => failure
        }

      }

    }

  }

}
