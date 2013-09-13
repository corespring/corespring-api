package tests.models

import org.specs2.mutable.Specification
import org.corespring.qti.models.QtiItem
import org.corespring.platform.core.models.itemSession.{SessionOutcome, ItemSession}
import org.corespring.qti.models.responses.StringResponse
import scalaz.{Failure, Success}
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.platform.core.models.error.InternalError

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
          println(sessionOutcome);
          success;
        }
        case _ => failure
      }
    }

    "return internal error processing ResponseProcessing returning no score" in {
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
        case success: Success[_, _] => failure("Should have failed because of missing score, but didn't")
        case failure: Failure[InternalError, _] => success
        case _ => failure("Did not produce failure of the correct type")
      }

    }

    "return internal error processing ResponseProcessing returning no isCorrect" in {
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
        case success: Success[_, _] => failure("Should have failed because of missing isCorrect, but didn't")
        case failure: Failure[InternalError, _] => success
        case _ => failure("Did not produce failure of the correct type")
      }
    }

    "return internal error processing ResponseProcessing returning no isComplete" in {
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
        case success: Success[_, _] => failure("Should have failed because of missing isComplete, but didn't")
        case failure: Failure[InternalError, _] => success
        case _ => failure("Did not produce failure of the correct type")
      }
    }

    "return internal error processing ResponseProcessing returning missing ResponseDeclaration identifier" in {
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
        case success: Success[_, _] =>
          failure("Should have failed because of missing ResponseDeclaration identifier, but didn't")
        case failure: Failure[InternalError, _] => success
        case _ => failure("Did not produce failure of the correct type")
      }
    }

    "return internal error processing ResponseProcessing returning missing score for ResponseDeclaration identifier" in {
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
        case success: Success[_, _] =>
          failure("Should have failed because of missing score for ResponseDeclaration identifier, but didn't")
        case failure: Failure[InternalError, _] => success
        case _ => failure("Did not produce failure of the correct type")
      }
    }

    "return internal error processing ResponseProcessing returning missing isCorrect for ResponseDeclaration identifier" in {
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
        case success: Success[_, _] =>
          failure("Should have failed because of missing isCorrect for ResponseDeclaration identifier, but didn't")
        case failure: Failure[InternalError, _] => success
        case _ => failure("Did not produce failure of the correct type")
      }
    }

    "return internal error processing ResponseProcessing returning missing isComplete for ResponseDeclaration identifier" in {
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
        case success: Success[_, _] =>
          failure("Should have failed because of missing isComplete for ResponseDeclaration identifier, but didn't")
        case failure: Failure[InternalError, _] => success
        case _ => failure("Did not produce failure of the correct type")
      }
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
          <inlineChiceInteraction responseIdentifier="Q_02">
            <inlineChoice identifier="1"/>
            <inlineChoice identifier="2"/>
          </inlineChiceInteraction>
        </itemBody>
      </assessmentItem>)
  }

}
