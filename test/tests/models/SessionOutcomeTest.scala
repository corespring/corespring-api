package tests.models

import org.specs2.mutable.Specification
import org.corespring.qti.models.QtiItem
import org.corespring.platform.core.models.itemSession.{SessionOutcome, ItemSession}
import org.corespring.qti.models.responses.StringResponse
import scalaz.Success
import org.corespring.platform.data.mongo.models.VersionedId

class SessionOutcomeTest extends Specification {

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
          println(outcome)
          outcome.score === 1.0
          outcome.isCorrect === true
          outcome.isComplete === true
          success
        }
        case _ => failure
      }

    }

    "return evaluated response processing with ResponseProcessing" in {
      val computedScore = 0.234235324
      val qtiItem =
        QtiItem(
          <assessmentItem>
            <responseProcessing type="script">
              <script type="text/javascript">
                {computedScore};
              </script>
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
          </assessmentItem>
        )

      val itemSession = ItemSession(
        itemId = VersionedId("50180807e4b0b89ebc0153b0").get,
        responses = Seq(StringResponse(id = "Q_01", responseValue = "2"), StringResponse(id = "Q_02", responseValue = "1")),
        attempts = 1)

      SessionOutcome.processSessionOutcome(itemSession, qtiItem) match {
        case Success(outcome) => {
          println(outcome)
          outcome.score === computedScore
          outcome.isCorrect === false
          outcome.isComplete === true
          success
        }
        case _ => failure
      }

      true === true
    }

  }

}
