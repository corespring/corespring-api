package tests.models

import org.specs2.mutable.Specification
import org.corespring.qti.models.QtiItem
import org.corespring.platform.core.models.itemSession.{ResponseProcessingOutputValidator, SessionOutcome, ItemSession}
import org.corespring.qti.models.responses.{ArrayResponse, StringResponse}
import scalaz.{Failure, Success}
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.platform.core.models.error.InternalError
import play.api.libs.json.{JsBoolean, JsObject, JsNumber}

class SessionOutcomeTest extends Specification {

  val itemSession = ItemSession(
    itemId = VersionedId("50180807e4b0b89ebc0153b0").get,
    responses = Seq(StringResponse(id = "Q_01", responseValue = "2"), StringResponse(id = "Q_02", responseValue = "1")),
    attempts = 1)

  "SessionOutcome" should {

    "return default scoring with no response processing" in {
      val qtiItem =
        QtiItem(
          <assessmentItem>
            <responseDeclaration identifier="Q_01" cardinality="single" baseType="identifier">
              <correctResponse>
                <value>2</value>
              </correctResponse>
            </responseDeclaration>
            <responseDeclaration identifier='Q_02' cardinality='single' baseType='identifier'>
              <correctResponse>
                <value>1</value>
              </correctResponse>
            </responseDeclaration>
            <itemBody>
              <inlineChoiceInteraction responseIdentifier="Q_01">
                <inlineChoice identifier="1"/>
                <inlineChoice identifier="2"/>
              </inlineChoiceInteraction>
              <inlineChiceInteraction responseIdentifier="Q_02">
                <inlineChoice identifier="1"/>
                <inlineChoice identifier="2"/>
              </inlineChiceInteraction>
            </itemBody>
          </assessmentItem>
        )

      val itemSession = ItemSession(
        itemId = VersionedId("50180807e4b0b89ebc0153b0").get,
        responses = Seq(StringResponse(id = "Q_01", responseValue = "2"), StringResponse(id = "Q_02", responseValue = "1")),
        attempts = 1)

      SessionOutcome.processSessionOutcome(itemSession, qtiItem) match {
        case Success(outcome) => {
          outcome.score === 1.0
          outcome.isCorrect === true
          outcome.isComplete === true
          success
        }
        case _ => failure
      }

    }

    "return evaluated response processing ResponseProcessing" in {
      val qtiItem = itemWithResponseJs(
        """
         var responses = itemSession['responses'];

         function arrayObjectIndexOf(responses, searchTerm, property) {
           var len = responses.length;
           for (var i = 0; i < len; i++) {
             if (responses[i][property] === searchTerm) {
               return i;
             }
           }
           return -1;
         }

         var questionOne = responses[arrayObjectIndexOf(responses, "Q_01", "id")];
         var questionTwo = responses[arrayObjectIndexOf(responses, "Q_02", "id")];

         var questionOneScore = (questionOne.value == "2") ? 1 : 0;
         var questionTwoScore = (questionTwo.value == "1") ? 1 : 0;
         var overallScore = (questionOneScore + questionTwoScore) / 2;

         var response = {
           score: overallScore,
           isCorrect: overallScore == 1,
           isComplete: overallScore == 1,
           Q_01: {
             score: questionOneScore,
             isCorrect: questionOneScore == 1,
             isComplete: questionOneScore == 1
           },
           Q_02: {
             score: questionTwoScore,
             isCorrect: questionTwoScore == 1,
             isComplete: questionTwoScore == 1
           }
         };
         response;
        """
      )

      SessionOutcome.processSessionOutcome(itemSession, qtiItem) match {
        case Success(sessionOutcome: SessionOutcome) => {
          sessionOutcome.score === 1.0
          sessionOutcome.isCorrect === true
          sessionOutcome.isComplete === true
          sessionOutcome.identifierOutcomes match {
            case Some(outcomes) => {
              outcomes.get("Q_01") match {
                case Some(q1Outcome) => {
                  q1Outcome.score === 1
                  q1Outcome.isCorrect === true
                  q1Outcome.isComplete === true
                  outcomes.get("Q_02") match {
                    case Some(q2Outcome) => {
                      q2Outcome.score === 1
                      q2Outcome.isCorrect === true
                      q2Outcome.isComplete === true
                      success
                    }
                    case _ => failure("SessionOutcome did not contain responseDeclaration identifier Q_02")
                  }
                }
                case _ => failure("SessionOutcome did not contain responseDeclaration identifier Q_01")
              }
            }
            case _ => failure("SessionOutcome did not contain values for responseDeclaration identifiers")
          }
        }
        case _ => failure("Did not correctly process response to SessionOutcome")
      }
    }

    "return default value for score when Javascript does not include it in response" in {
      val qtiItem = itemWithResponseJs(
        """
          var response = {
            isCorrect: true,
            isComplete: true,
            Q_01: {
              score: 1,
              isCorrect: true,
              isComplete: true
            },
            Q_02: {
              score: 1,
              isCorrect: true,
              isComplete: true
            }
          };
          response;
        """
      )

      SessionOutcome.processSessionOutcome(itemSession, qtiItem) match {
        case s: Success[_, _] => success
        case _ => failure("Should have included score, but didn't")
      }

    }

    "return default value for isCorrect when Javascript does not include it in response" in {
      val qtiItem = itemWithResponseJs(
        """
          var response = {
            score: 1,
            isComplete: true,
            Q_01: {
              score: 1,
              isCorrect: true,
              isComplete: true
            },
            Q_02: {
              score: 1,
              isCorrect: true,
              isComplete: true
            }
          };
          response;
        """
      )

      SessionOutcome.processSessionOutcome(itemSession, qtiItem) match {
        case s: Success[_, _] => success
        case _ => failure("Should have included isCorrect, but didn't")
      }
    }


    "return default value for isComplete when Javascript does not include it in response" in {
      val qtiItem = itemWithResponseJs(
        """
          var response = {
            score: 1,
            isCorrect: true,
            Q_01: {
              score: 1,
              isCorrect: true,
              isComplete: true
            },
            Q_02: {
              score: 1,
              isCorrect: true,
              isComplete: true
            }
          };
          response;
        """
      )

      SessionOutcome.processSessionOutcome(itemSession, qtiItem) match {
        case s: Success[_, _] => success
        case _ => failure("Should have included isComplete, but didn't")
      }
    }

    "return default values for response identifier when Javascript does not include it in response" in {
      val qtiItem = itemWithResponseJs(
        """
          var response = {
            score: 1,
            isCorrect: true,
            isComplete: true,
            Q_02: {
              score: 1,
              isCorrect: true,
              isComplete: true
            }
          };
          response;
        """
      )

      SessionOutcome.processSessionOutcome(itemSession, qtiItem) match {
        case s: Success[_, _] => success
        case _ => failure("Should have contained response identifier Q_01, but didn't")
      }
    }

    "return default value for score in response identifier when Javascript does not include it in response" in {
      val qtiItem = itemWithResponseJs(
        """
          var response = {
            score: 1,
            isCorrect: true,
            isComplete: true,
            Q_01: {
              isCorrect: true,
              isComplete: true
            },
            Q_02: {
              score: 1,
              isCorrect: true,
              isComplete: true
            }
          };
          response;
        """
      )

      SessionOutcome.processSessionOutcome(itemSession, qtiItem) match {
        case s: Success[_, _] => success
        case _ => failure("Should have contained score, but didn't")
      }
    }

    "return default value for isCorrect in response identifier when Javascript does not include it in response" in {
      val qtiItem = itemWithResponseJs(
        """
          var response = {
            score: 1,
            isCorrect: true,
            isComplete: true,
            Q_01: {
              score: 1,
              isComplete: true
            },
            Q_02: {
              score: 1,
              isCorrect: true,
              isComplete: true
            }
          };
          response;
        """
      )

      SessionOutcome.processSessionOutcome(itemSession, qtiItem) match {
        case s: Success[_, _] => success
        case _ => failure("Should have contained Q_01.isCorrect, but didn't")
      }
    }

    "return default value for isComplete in response identifier when Javascript does not include it in response" in {
      val qtiItem = itemWithResponseJs(
        """
          var response = {
            score: 1,
            isCorrect: true,
            isComplete: true,
            Q_01: {
              score: 1,
              isCorrect: true
            },
            Q_02: {
              score: 1,
              isCorrect: true,
              isComplete: true
            }
          };
          response;
        """
      )

      SessionOutcome.processSessionOutcome(itemSession, qtiItem) match {
        case s: Success[_, _] => success
        case _ => failure("Should have contained Q_01.isCompleted, but didn't")
      }
    }

    "contain preprocessed Javascript for InlineChoiceInteraction" in {
      val script =
        """
          var score = (badStepQuestion.value == '1') ? 1 : 0;
          var response = {
            score: score,
            isCorrect: score == 1,
            isComplete: score == 1,
            badStepQuestion: {
              score: score,
              isCorrect: score == 1,
              isComplete: score == 1
            }
          };
          response;
        """

      val qtiItem = QtiItem(
        <assessmentItem>
          <responseProcessing type="script">
            <script type="text/javascript">{script}</script>
          </responseProcessing>
          <responseDeclaration identifier='badStepQuestion' cardinality='single' baseType='identifier'>
            <correctResponse>
              <value>1</value>
            </correctResponse>
          </responseDeclaration>
          <itemBody>
            <inlineChoiceInteraction responseIdentifier='badStepQuestion'></inlineChoiceInteraction>
          </itemBody>
        </assessmentItem>
      )

      val itemSession = ItemSession(
        itemId = VersionedId("50180807e4b0b89ebc0153b0").get,
        responses = Seq(StringResponse(id = "badStepQuestion", responseValue = "1"))
      )

      SessionOutcome.processSessionOutcome(itemSession, qtiItem) match {
        case s: Success[InternalError, SessionOutcome] => {
          s.getOrElse(null) match {
            case sessionOutcome: SessionOutcome => {
              sessionOutcome.isComplete === true
              sessionOutcome.isCorrect === true
              sessionOutcome.score === 1
              sessionOutcome.identifierOutcomes match {
                case Some(outcomes) => {
                  outcomes.get("badStepQuestion") match {
                    case Some(outcome) => {
                      outcome.isComplete === true
                      outcome.isCorrect === true
                      outcome.score === 1
                      success
                    }
                    case _ => failure("No outcome for identifier badStepQuestion")
                  }
                }
                case None => failure("No outcomes for identifiers")
              }
            }
            case _ => failure
          }

        }
        case f: Failure[InternalError, _] => failure
      }
    }

    "contain preprocessed Javascript for DragAndDropInteraction" in {
      val script =
        """
          var score = dragDropQuestion.value.target1 == '1' ? 1 : 0;
          score += dragDropQuestion.value.target2 == '2' ? 1 : 0;
          var response = {
            score: score / 2,
            isCorrect: score == 2,
            isComplete: score == 2,
            dragDropQuestion: {
              score: score / 2,
              isCorrect: score == 2,
              isComplete: score == 2
            }
          };
          response;
        """
      val qtiItem = QtiItem(
        <assessmentItem>
          <responseProcessing type="script">
            <script type="text/javascript">{script}</script>
          </responseProcessing>
          <responseDeclaration identifier='dragDropQuestion' cardinality='targeted' baseType='identifier'>
            <correctResponse>
              <value identifier='target1'>1</value>
              <value identifier='target2'>2</value>
            </correctResponse>
          </responseDeclaration>
          <itemBody>
            <dragAndDropInteraction responseIdentifier='dragDropQuestion' orderMatters='true'>
              <draggableChoice identifier='1'/>
              <draggableChoice identifier='2'/>
              <landingPlace cardinality='single' identifier='target1'/>
              <landingPlace cardinality='single' identifier='target2'/>
            </dragAndDropInteraction>
          </itemBody>
        </assessmentItem>
      )

      val itemSession = ItemSession(
        itemId = VersionedId("50180807e4b0b89ebc0153b0").get,
        responses = Seq(ArrayResponse(id = "dragDropQuestion", responseValue = Seq("target1:1", "target2:2"))),
        attempts = 1)

      SessionOutcome.processSessionOutcome(itemSession, qtiItem) match {
        case s: Success[InternalError, SessionOutcome] => {
          s.getOrElse(null) match {
            case sessionOutcome: SessionOutcome => {
              sessionOutcome.isComplete === true
              sessionOutcome.isCorrect === true
              sessionOutcome.score === 1
              sessionOutcome.identifierOutcomes match {
                case Some(outcomes) => {
                  outcomes.get("dragDropQuestion") match {
                    case Some(outcome) => {
                      outcome.isComplete === true
                      outcome.isCorrect === true
                      outcome.score === 1
                      success
                    }
                    case _ => failure("No outcome for identifier dragDropQuestion")
                  }
                }
                case None => failure("No outcomes for identifiers")
              }
            }
            case _ => failure("No SessionOutcome")
          }
        }
        case f: Failure[InternalError, _] => failure("There was an error processing the Javascript")
      }
    }

  }

  "contain default response in outcome" in {
    val qtiItem = itemWithResponseJs(
      """
        var returnValue = {
          score: (Q_01.outcome.score + Q_02.outcome.score) / 2,
          isCorrect: (Q_01.outcome.isCorrect && Q_02.outcome.isCorrect),
          isComplete: (Q_01.outcome.isComplete && Q_02.outcome.isComplete),
          Q_01: {
            score: parseInt(Q_01.value),
            isCorrect: false,
            isComplete: false
          },
          Q_02: {
            score: parseInt(Q_02.value),
            isCorrect: true,
            isComplete: true
          }
        };
        returnValue;
      """)

    val itemSession = ItemSession(
      itemId = VersionedId("50180807e4b0b89ebc0153b0").get,
      responses = Seq(StringResponse(id = "Q_01", responseValue = "2"), StringResponse(id = "Q_02", responseValue = "1"))
    )

    SessionOutcome.processSessionOutcome(itemSession, qtiItem) match {
      case Success(sessionOutcome: SessionOutcome) => {
        sessionOutcome.score === 1
        sessionOutcome.isComplete === true
        sessionOutcome.isCorrect === true
        sessionOutcome.identifierOutcomes match {
          case Some(outcomes) => {
            outcomes.get("Q_01") match {
              case Some(q1Outcome) => {
                q1Outcome.score === 2
                q1Outcome.isCorrect === false
                q1Outcome.isComplete === false

                outcomes.get("Q_02") match {
                  case Some(q2Outcome) => {
                    q2Outcome.score === 1
                    q2Outcome.isCorrect === true
                    q2Outcome.isComplete === true
                    success
                  }
                  case _ => failure("No outcome for identifier Q_02")
                }
              }
              case _ => failure("No outcome for identifier Q_01")
            }
          }
          case _ => failure("No identifier outcomes found")
        }
        success
      }
      case _ => failure
    }

  }

  private def itemWithResponseJs(js: String): QtiItem = {
    QtiItem(
      <assessmentItem>
        <responseProcessing type="script">
          <script type="text/javascript">{js}</script>
        </responseProcessing>
        <responseDeclaration identifier="Q_01" cardinality="single" baseType="identifier">
          <correctResponse>
            <value>2</value>
          </correctResponse>
        </responseDeclaration>
        <responseDeclaration identifier='Q_02' cardinality='single' baseType='identifier'>
          <correctResponse>
            <value>1</value>
          </correctResponse>
        </responseDeclaration>
        <itemBody>
          <inlineChoiceInteraction responseIdentifier="Q_01">
            <inlineChoice identifier="1"/>
            <inlineChoice identifier="2"/>
          </inlineChoiceInteraction>
          <inlineChoiceInteraction responseIdentifier="Q_02">
            <inlineChoice identifier="1"/>
            <inlineChoice identifier="2"/>
          </inlineChoiceInteraction>
        </itemBody>
      </assessmentItem>)
  }

}
