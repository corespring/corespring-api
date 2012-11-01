package models

import qti.models.QtiItem
import qti.models.QtiItem.Correctness

object Score {

  def scoreResponses(responses: Seq[ItemResponse], qti: QtiItem): Seq[ItemResponse] = {

    def addResponseOutcome(r: ItemResponse) : ItemResponse  = r match {
      case StringItemResponse(id,value, _) => ItemResponse(r, correctnessToOutcome(qti.isCorrect(id,value)))
      case ArrayItemResponse(id,value, _) => ItemResponse(r, correctnessToOutcome(qti.isCorrect(id,value.mkString(","))))
    }

    responses.map(addResponseOutcome)
  }

  private def correctnessToOutcome(c : Correctness.Value) : Option[ItemResponseOutcome] = c match {
    case Correctness.Correct => Some(ItemResponseOutcome(1))
    case Correctness.Incorrect => Some(ItemResponseOutcome(0))
    case _ => None
  }

}
