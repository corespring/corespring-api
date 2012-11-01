package models

import qti.models.QtiItem

object Score {

  def scoreResponses(responses: Seq[ItemResponse], qti: QtiItem): Seq[ItemResponse] = {

    def addResponseOutcome(r: ItemResponse) : ItemResponse  = {
      val score = if (qti.isCorrect(r.id, r.value)) 1 else 0
      val outcome = ItemResponseOutcome(score = score)
      ItemResponse(r, outcome)
    }
    responses.map(addResponseOutcome)
  }

}
