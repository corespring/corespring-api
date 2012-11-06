package models

import qti.models.QtiItem
import qti.models.QtiItem.Correctness

object Score {

  def scoreResponses(responses: Seq[ItemResponse], qti: QtiItem, addReport : Boolean = true): Seq[ItemResponse] = {

    def addResponseOutcome(r: ItemResponse): ItemResponse = {
      r match {
        case StringItemResponse(id, value, _) => {
          val outcome = correctnessToOutcome(qti.isCorrect(id, value) )
          ItemResponse(r, outcome)
        }
        case ArrayItemResponse(id, value, _) => {
          val outcome = correctnessToOutcome(qti.isCorrect(id, value.mkString(",")) )
          ItemResponse(r, outcome)
        }
      }
    }
    responses.map(addResponseOutcome)
  }

  private def correctnessToOutcome(c: Correctness.Value ): Option[ItemResponseOutcome] = c match {
    case Correctness.Correct => Some(ItemResponseOutcome(1))
    case Correctness.Incorrect => Some(ItemResponseOutcome(0))
    case _ => None
  }


}
