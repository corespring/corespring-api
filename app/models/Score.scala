package models

import qti.models.QtiItem

object Score {

  def scoreResponses(responses: Seq[ItemResponse], qti: QtiItem): Seq[ItemResponse] = {

    def addResponseOutcome(r: ItemResponse) : ItemResponse  = r match {



      case StringItemResponse(id,value, _) => {
        val i : ItemResponse = StringItemResponse("d","b")

        val score = if (qti.isCorrect(id, value)) 1 else 0
        val outcome = ItemResponseOutcome(score = score)
         ItemResponse(r, outcome)
      }
      case ArrayItemResponse(id,value, _) => {
        val score = if (qti.isCorrect(id, value.mkString(","))) 1 else 0
        val outcome = ItemResponseOutcome(score = score)
        ItemResponse(r, outcome)
      }
    }
    responses.map(addResponseOutcome)
  }

}
