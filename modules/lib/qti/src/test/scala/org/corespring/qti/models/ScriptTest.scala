package org.corespring.qti.models

import org.specs2.mutable.Specification
import org.mozilla.javascript.EvaluatorException
import play.api.libs.json._

class ScriptTest extends Specification {

  "Script" should {

    "throw exception for script of unknown type" in {
      val scriptNode =
        <script type="text/coffeescript">
          (myFunction) -> console.log("Hello CoffeeScript!")
        </script>
      Script(scriptNode) must throwAn[IllegalArgumentException]
    }

    "execute" should {

      "throw EcmaError for script with invalid Javascript" in {
        val scriptNode =
          <script type="text/javascript">
            (myFunction) -> console.log("Still Coffeescript!")
          </script>
        Script(scriptNode).execute must throwAn[EvaluatorException]
      }

      "return doubles properly" in {
        val a = 5
        val b = 6
        val scriptNode =
          <script type="text/javascript">
            var a = {a};
            var b = {b};
            a + b;
          </script>

        Script(scriptNode).execute match {
          case Some((script:String, jsNumber: JsNumber)) if jsNumber.value == (a + b) => success
          case _ => failure
        }
      }

      "return strings properly" in {
        val a = "Ben"
        val b = "Burton"
        val scriptNode =
          <script type="text/javascript">
            var a = "{a}";
            var b = "{b}";
            a + " " + b;
          </script>

        Script(scriptNode).execute match {
          case Some((script:String,jsString: JsString)) if jsString.value == s"$a $b" => success
          case _ => failure
        }
      }

      "return JSObject properly" in {
        val scriptNode =
          <script type="text/javascript">
            {
            """
              var result = {
                score: 5
              };
              result;
            """}
          </script>

        Script(scriptNode).execute match {
          case Some((script:String,jsObject: JsObject)) if (jsObject \ "score").asInstanceOf[JsNumber].value == 5 => success
          case _ => failure
        }

      }

      "with variables" should {

        "make strings available in scope" in {
          val scriptNode =
            <script type="text/javascript">
              {
              """
                var o = {
                  one: one,
                  two: two
                };
                o;
              """
              }

            </script>
          val one = "one"
          val two = "two"

          Script(scriptNode).execute(Map("one" -> one, "two" -> two)) match {
            case Some((script:String,jsObject: JsObject)) => {
              (jsObject \ "one").asInstanceOf[JsString].value === "one"
              (jsObject \ "two").asInstanceOf[JsString].value === "two"
              success
            }
            case _ => failure
          }
        }

        "make integers available in scope" in {
          val scriptNode =
            <script type="text/javascript">
              one + two;
            </script>
          val one = 1
          val two = 2

          Script(scriptNode).execute(Map("one" -> one, "two" -> two)) match {
            case Some((script:String,jsNumber: JsNumber)) if jsNumber.value == (one + two) => success
            case _ => failure
          }

        }

        "make arrays available in scope" in {
          val scriptNode =
            <script type="text/javascript">
              {"""
                  var answer = "";
                  for (i in array) {
                    answer += array[i];
                    if (i != array.length-1) {
                      answer += " ";
                    }
                  }
                  answer;
               """}
            </script>

          val array = Json.arr("one", "two", "three")

          Script(scriptNode).execute(Map("array" -> array)) match {
            case Some((script:String,jsString: JsString)) if jsString.value == "one two three" => success
            case _ => failure
          }
          true === true
        }

        "make objects available in scope" in {

          val scriptNode = <script type="text/javascript">
            {"""
                var overallScore = 0;
                if (dragDropQuestion.outcome.isCorrect) overallScore += .60;
                if (selectBadStep.outcome.isCorrect) overallScore += .40;
                var v = {
                  score: overallScore,
                  isCorrect: true
                };
                v;
             """}
          </script>

          val dragDropQuestion = Json.obj( "outcome" -> Json.obj("isCorrect" -> true))
          val selectBadStep = Json.obj("outcome" -> Json.obj("isCorrect" -> false))

          val result = Script(scriptNode).execute(Map("dragDropQuestion" -> dragDropQuestion, "selectBadStep" -> selectBadStep))

          result match {
            case Some((script:String,jsObject: JsObject)) => {
              (jsObject \ "score").asInstanceOf[JsNumber].value === BigDecimal(0.60)
              (jsObject \ "isCorrect").asInstanceOf[JsBoolean].value === true
              success
            }
            case _ => failure
          }

        }

      }

    }

  }

}
